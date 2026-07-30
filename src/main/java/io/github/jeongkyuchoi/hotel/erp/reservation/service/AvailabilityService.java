package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.booking.dto.AvailabilityResponse;
import io.github.jeongkyuchoi.hotel.erp.booking.dto.AvailabilityResponse.NightAvailability;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가용 재고 조회 — 부킹엔진 표시 경로(D-030).
 *
 * <p><b>이 경로는 절대 락을 걸지 않는다(D-018).</b> 예약 확정의 락 조회
 * ({@code lockForUpdateNative})와 엄격히 분리된 <b>표시 전용</b> 경로다. 확정 트랜잭션에서
 * 이 메서드를 부르면, 여기서 영속성 컨텍스트에 올라온 재고 인스턴스가 뒤이은 락 조회의
 * 최신값을 가려 오버부킹이 난다(D-018 실측). 그래서 읽기 전용 트랜잭션으로 못 박는다.
 *
 * <p><b>가용 = 총량 − 확정 − 점유(raw).</b> 만료됐지만 아직 스케줄러가 정리하지 못한
 * HOLD 의 {@code held_qty} 도 점유로 잡힌다. 이상적으로는 조회 시점 만료분을 빼야 하지만
 * (D-003), 그 계산은 만료 HOLD 를 일자별로 집계하는 조인이 필요하다. 스케줄러 주기가 1분
 * 이라 과다표시(실제보다 가용을 적게 보여줌)의 지속이 그 안이고, 안전한 방향(덜 팔림)의
 * 오차라 표시 경로에서는 raw 로 둔다. 확정은 어차피 락으로 정확히 막는다.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

	private static final Long TENANT_ID = 1L;

	private final RoomInventoryRepository roomInventoryRepository;

	@Transactional(readOnly = true)
	public AvailabilityResponse check(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
		int nightCount = (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
		if (nightCount < 1) {
			throw new IllegalArgumentException(
					"체크아웃은 체크인 다음날 이후여야 합니다. in=" + checkInDate + " out=" + checkOutDate);
		}
		LocalDate firstNight = checkInDate;
		LocalDate lastNight = checkOutDate.minusDays(1);

		// 표시용 무락 조회. 확정 경로의 lockForUpdateNative 와 다른 메서드다(D-018).
		Map<LocalDate, RoomInventory> byDate = new HashMap<>();
		for (RoomInventory inv : roomInventoryRepository
				.findByTenantIdAndRoomTypeIdAndStayDateBetweenOrderByStayDate(
						TENANT_ID, roomTypeId, firstNight, lastNight)) {
			byDate.put(inv.getStayDate(), inv);
		}

		List<NightAvailability> nights = new ArrayList<>();
		int bookable = Integer.MAX_VALUE;
		for (LocalDate d = firstNight; !d.isAfter(lastNight); d = d.plusDays(1)) {
			RoomInventory inv = byDate.get(d);
			int available = inv != null ? inv.availableQty() : 0; // 재고 행 없으면 판매 불가
			nights.add(new NightAvailability(d, available));
			bookable = Math.min(bookable, available); // 연박은 최소 가용에 묶인다
		}

		return new AvailabilityResponse(
				roomTypeId, checkInDate, checkOutDate, nightCount, bookable, nights);
	}
}
