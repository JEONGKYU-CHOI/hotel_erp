package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.notification.event.ReservationConfirmedEvent;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HOLD → CONFIRMED 확정. 결제 성공 콜백이 부를 도메인 전이다.
 *
 * <p>임시점유를 확정으로 바꾼다 — 재고의 {@code held_qty} 를 {@code sold_qty} 로 옮긴다.
 * 총량은 그대로라 CHECK 제약을 새로 위협하지 않는다(점유분이 이미 총량 안에 있었다).
 *
 * <p><b>확정 vs 만료 경합</b> — 결제가 HOLD 만료와 거의 동시에 도착할 수 있다. 그래서
 * 예약 행을 {@code FOR UPDATE} 로 먼저 잠가(D-025) 전이를 직렬화한다. 만료 서비스도 같은
 * 행을 같은 순서(예약 → 재고)로 잠그므로, 둘 중 하나만 성공하고 재고는 정확히 한 번만
 * 이동한다.
 *
 * <p><b>멱등</b> — 결제 웹훅은 재시도된다. 이미 CONFIRMED 면 아무 것도 하지 않고 그대로
 * 돌려준다. 두 번째 호출이 재고를 또 옮기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationConfirmService {

	private final ReservationRepository reservationRepository;
	private final RoomInventoryRepository roomInventoryRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public Reservation confirm(Long reservationId) {
		// ★ 예약 행을 먼저 잠근다(D-025). 만료와의 경합에서 이 락이 직렬화 지점이다.
		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new NotFoundException(
						"예약을 찾을 수 없습니다. id=" + reservationId));

		// 멱등 — 웹훅 재시도. 이미 확정됐으면 재고를 또 옮기지 않는다.
		if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
			log.info("확정 재요청(멱등) — no={}", reservation.getReservationNo());
			return reservation;
		}

		LocalDate firstNight = reservation.getCheckInDate();
		LocalDate lastNight = reservation.getCheckOutDate().minusDays(1);

		// 재고 행 잠금(예약 다음, D-025 락 순서). held → sold 이동 대상이다.
		Map<LocalDate, RoomInventory> inventoryByDate = new HashMap<>();
		for (RoomInventory inv : roomInventoryRepository.lockForUpdateNative(
				reservation.getRoomType().getId(), firstNight, lastNight)) {
			inventoryByDate.put(inv.getStayDate(), inv);
		}

		for (ReservationNight night : reservation.getNights()) {
			RoomInventory inv = inventoryByDate.get(night.getStayDate());
			if (inv == null) {
				// 확정 시점에 재고 행이 없다는 것은 데이터 정합성이 깨진 것이다.
				throw new IllegalStateException(
						night.getStayDate() + " 재고 행이 없어 확정할 수 없습니다. no="
								+ reservation.getReservationNo());
			}
			inv.confirmHold(1); // held-- , sold++
		}

		reservation.confirm(); // HOLD 가 아니면 여기서 예외 → 만료된 뒤 도착한 결제는 거부
		log.info("확정: no={} {}~{} 총액={}",
				reservation.getReservationNo(), reservation.getCheckInDate(),
				reservation.getCheckOutDate(), reservation.getTotalAmount());
		// 커밋 후 예약 확정 안내 메일. 실제 전이가 일어난 이 경로에서만 발행한다(멱등 재확정 제외).
		eventPublisher.publishEvent(new ReservationConfirmedEvent(reservation.getId()));
		return reservation;
	}
}
