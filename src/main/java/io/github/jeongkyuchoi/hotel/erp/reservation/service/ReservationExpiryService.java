package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 만료된 HOLD 를 EXPIRED 로 정리하고 재고를 되돌린다(D-003).
 *
 * <p>HOLD 는 임시점유를 겸하므로(D-003) 만료 정리 = {@code held_qty} 반환이다. 이 서비스는
 * {@link HoldExpiryScheduler}가 주기적으로 부른다. 조회 시점에는 이미 만료분을 만료로
 * 취급하므로(Reservation#isHoldExpired) 이 정리는 <b>지연된 청소</b>일 뿐, 사용자가 보는
 * 가용 재고에는 영향을 주지 않는다.
 *
 * <p><b>왜 만료도 재고를 락으로 잡는가</b> — {@code releaseHold} 는 {@code held_qty} 를
 * 내린다. 같은 재고 행을 놓고 새 예약의 {@code hold}(증가)와 동시에 다투므로, 락 없이
 * 읽으면 REPEATABLE READ 스냅샷의 옛 값에 대고 증감해 갱신이 유실된다(D-018 과 같은 함정).
 * 그래서 되돌릴 행을 {@code FOR UPDATE} 로 먼저 잠근다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationExpiryService {

	private static final Long TENANT_ID = 1L;

	private final ReservationRepository reservationRepository;
	private final RoomInventoryRepository roomInventoryRepository;

	/** 정리 대상 id 조회 (읽기 전용). 실제 처리는 {@link #expireOne} 이 건마다 맡는다. */
	@Transactional(readOnly = true)
	public List<Long> findDueHoldIds(LocalDateTime now, int batchLimit) {
		return reservationRepository.findDueHoldIds(
				TENANT_ID, ReservationStatus.HOLD, now, Limit.of(batchLimit));
	}

	/**
	 * 예약 한 건을 만료 처리한다. <b>건마다 독립 트랜잭션</b>이라 한 건이 실패해도
	 * 나머지에 영향을 주지 않고, 재고 락 보유 시간도 짧게 유지된다.
	 *
	 * <p>멱등하다 — 이미 확정·취소·만료됐거나 아직 만료 전이면 아무 것도 하지 않고 넘어간다.
	 * 스케줄러가 겹쳐 돌거나, 결제 확정과 경합해 이 예약이 그 사이 CONFIRMED 로 바뀌었어도
	 * 안전하다.
	 */
	@Transactional
	public void expireOne(Long reservationId, LocalDateTime now) {
		Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
		if (reservation == null || !reservation.isHoldExpired(now)) {
			// 경합에서 졌거나(확정됨) 아직 만료 전. 조용히 통과하는 것이 멱등성이다.
			return;
		}

		LocalDate firstNight = reservation.getCheckInDate();
		LocalDate lastNight = reservation.getCheckOutDate().minusDays(1);

		// ★ 되돌릴 재고 행을 먼저 잠근다(D-018). 이 트랜잭션에서 재고 첫 조회 = 락 조회.
		Map<LocalDate, RoomInventory> inventoryByDate = new HashMap<>();
		for (RoomInventory inv : roomInventoryRepository.lockForUpdateNative(
				reservation.getRoomType().getId(), firstNight, lastNight)) {
			inventoryByDate.put(inv.getStayDate(), inv);
		}

		for (ReservationNight night : reservation.getNights()) {
			RoomInventory inv = inventoryByDate.get(night.getStayDate());
			if (inv != null) {
				inv.releaseHold(1);
			}
			// 재고 행이 사라진 경우는 되돌릴 대상이 없다. HOLD 중 재고 삭제는 일어나지
			// 않지만, 방어적으로 건너뛰고 예약 상태 전이는 그대로 진행한다.
		}

		reservation.expire();
		log.info("HOLD 만료: no={} {}~{} 재고 반환 완료",
				reservation.getReservationNo(),
				reservation.getCheckInDate(), reservation.getCheckOutDate());
	}
}
