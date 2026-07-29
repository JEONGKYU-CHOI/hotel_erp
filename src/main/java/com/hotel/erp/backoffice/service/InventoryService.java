package com.hotel.erp.backoffice.service;

import com.hotel.erp.backoffice.dto.InventoryGenerateForm;
import com.hotel.erp.backoffice.dto.InventoryGenerateResult;
import com.hotel.erp.common.domain.basedata.RoomRepository;
import com.hotel.erp.common.domain.basedata.RoomType;
import com.hotel.erp.common.domain.basedata.RoomTypeRepository;
import com.hotel.erp.common.domain.inventory.RoomInventory;
import com.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import com.hotel.erp.common.exception.NotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일자별 재고 생성·조회.
 *
 * <p>물량형(사전 생성) 모델이다. 계산형(예약 COUNT)은 잠글 대상 행이 없어 동시성
 * 제어가 불가능하다(D-003 논의). 그 대가로 이 배치가 필요하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

	private static final Long TENANT_ID = 1L;

	private final RoomInventoryRepository roomInventoryRepository;
	private final RoomTypeRepository roomTypeRepository;
	private final RoomRepository roomRepository;

	/** 화면 표시용 조회. 락을 걸지 않는다. */
	public List<RoomInventory> findRange(Long roomTypeId, LocalDate from, LocalDate to) {
		return roomInventoryRepository
				.findByTenantIdAndRoomTypeIdAndStayDateBetweenOrderByStayDate(
						TENANT_ID, roomTypeId, from, to);
	}

	/** 해당 객실타입의 사용 중인 호실 수. 판매 총량의 기본값이다. */
	public int countActiveRooms(Long roomTypeId) {
		return (int) roomRepository.countByRoomTypeIdAndActiveTrue(roomTypeId);
	}

	/**
	 * 기간에 대해 재고 행을 만든다. 이미 있으면 판매 총량만 갱신한다.
	 *
	 * <p><b>왜 기존 행을 락 조회로 먼저 읽는가 (D-018)</b><br>
	 * 이 작업은 예약과 같은 행을 건드린다. 락 없이 읽으면 MySQL REPEATABLE READ
	 * 스냅샷의 옛 {@code sold_qty} 를 보게 되고, 그 값으로 "총량을 줄여도 되는가"를
	 * 판단하게 된다. 판단한 뒤 저장할 때는 실제 값이 더 커져 있어 DB CHECK 제약에
	 * error 3819 로 거부되거나, 더 나쁘게는 통과해 버린다.
	 * 그래서 <b>이 트랜잭션에서 재고 행을 처음 만나는 순간부터</b> FOR UPDATE 다.
	 * 나중에 락을 덧붙이는 것으로는 되돌릴 수 없다는 것을 Day 0 스파이크에서 실측했다.
	 *
	 * <p>부수 효과로, 재고 생성이 도는 동안 그 기간의 예약은 잠깐 대기한다.
	 * 기간을 {@link InventoryGenerateForm#MAX_DAYS} 로 제한한 이유이기도 하다.
	 */
	@Transactional
	public InventoryGenerateResult generate(InventoryGenerateForm form) {
		RoomType roomType = roomTypeRepository
				.findByIdAndTenantId(form.getRoomTypeId(), TENANT_ID)
				.orElseThrow(() -> new NotFoundException(
						"객실타입을 찾을 수 없습니다. id=" + form.getRoomTypeId()));

		int totalQty = form.getTotalQty() != null
				? form.getTotalQty()
				: countActiveRooms(roomType.getId());

		LocalDate from = form.getFromDate();
		LocalDate to = form.getToDate();

		// ★ 첫 조회부터 락이다(D-018). 아래에서 이 엔티티들을 그대로 쓴다.
		Map<LocalDate, RoomInventory> existing = roomInventoryRepository
				.lockForUpdateNative(roomType.getId(), from, to).stream()
				.collect(Collectors.toMap(RoomInventory::getStayDate, Function.identity()));

		int created = 0;
		int updated = 0;
		List<InventoryGenerateResult.Skipped> skipped = new ArrayList<>();
		List<RoomInventory> toInsert = new ArrayList<>();

		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			RoomInventory inventory = existing.get(date);

			if (inventory == null) {
				toInsert.add(RoomInventory.builder()
						.tenantId(TENANT_ID)
						.roomTypeId(roomType.getId())
						.stayDate(date)
						.totalQty(totalQty)
						.soldQty(0)
						.heldQty(0)
						.build());
				created++;
				continue;
			}

			if (inventory.getTotalQty() == totalQty) {
				skipped.add(new InventoryGenerateResult.Skipped(date, "이미 같은 수량입니다."));
				continue;
			}

			// 이미 팔렸거나 점유된 만큼은 줄일 수 없다. DB CHECK 가 최후에 막지만,
			// 거기까지 가면 트랜잭션 전체가 깨진다. 여기서 걸러 나머지 날짜는 살린다.
			if (!inventory.canChangeTotalQtyTo(totalQty)) {
				skipped.add(new InventoryGenerateResult.Skipped(date,
						"판매·점유 " + inventory.committedQty() + "건이 있어 "
								+ totalQty + "(으)로 줄일 수 없습니다."));
				continue;
			}

			inventory.changeTotalQty(totalQty);
			updated++;
		}

		// 변경 감지로 UPDATE 는 자동이고, 신규만 저장한다.
		// batch_size=50 이 설정돼 있어 INSERT 가 묶여 나간다.
		roomInventoryRepository.saveAll(toInsert);

		log.info("재고 생성: roomType={} 기간={}~{} 총량={} 생성={} 갱신={} 건너뜀={}",
				roomType.getCode(), from, to, totalQty, created, updated, skipped.size());

		return new InventoryGenerateResult(created, updated, skipped);
	}
}
