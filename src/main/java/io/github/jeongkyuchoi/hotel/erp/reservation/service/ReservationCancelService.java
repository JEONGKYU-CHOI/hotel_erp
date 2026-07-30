package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.CancellationCharge;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.CancellationPolicy;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 취소 — HOLD/CONFIRMED 를 CANCELLED 로 전이하고 재고를 되돌린다(D-027).
 *
 * <p><b>되돌릴 버킷은 취소 직전 상태가 결정한다.</b> HOLD 취소는 {@code held_qty} 를,
 * CONFIRMED 취소는 {@code sold_qty} 를 반환한다. 그래서 상태를 바꾸기 <b>전에</b> 먼저 읽어
 * 어느 버킷을 되돌릴지 정한 뒤, {@link Reservation#cancel} 로 전이한다.
 *
 * <p><b>락 순서는 예약 → 재고(D-025).</b> 확정·만료와 같은 순서를 지킨다. 예약 행을 먼저
 * {@code FOR UPDATE} 로 잠가, 취소가 확정·만료와 같은 예약을 두고 경합할 때 직렬화된다 —
 * 만료가 먼저 커밋했다면 여기 락 읽기가 EXPIRED 를 보고 아래 가드에서 거부되고, 취소가
 * 먼저면 만료 쪽이 CANCELLED 를 보고 무동작(멱등)한다. 재고 반환은 그 다음
 * {@code FOR UPDATE} 로 잠근 행에 대고 한다(D-018 — 락 없이 읽으면 갱신 유실).
 *
 * <p><b>멱등</b> — 이미 CANCELLED 면 재고를 또 되돌리지 않고 저장된 위약금 스냅샷을 그대로
 * 돌려준다. 재시도·더블클릭에 안전하다.
 *
 * <p><b>위약금(D-037)</b> — 확정(결제된) 예약 취소는 {@link CancellationPolicy} 로 위약금·환불을
 * 계산해 예약에 굳힌다. HOLD 취소는 미결제라 위약금 0. 실제 환불 실행(토스 결제취소)은 후속 범위.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationCancelService {

	private final ReservationRepository reservationRepository;
	private final RoomInventoryRepository roomInventoryRepository;

	@Transactional
	public CancellationCharge cancel(Long reservationId, String reason) {
		// ★ 예약 행을 먼저 잠근다(D-025). 확정·만료와 같은 순서(예약 → 재고)로 직렬화한다.
		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new NotFoundException(
						"예약을 찾을 수 없습니다. id=" + reservationId));

		// 멱등 — 이미 취소됐으면 재고를 또 되돌리지 않고 저장된 위약금을 그대로 돌려준다.
		if (reservation.getStatus() == ReservationStatus.CANCELLED) {
			log.info("취소 재요청(멱등) — no={}", reservation.getReservationNo());
			return CancellationCharge.settled(
					reservation.getCancellationFee(), reservation.getTotalAmount());
		}

		// ★ 되돌릴 버킷은 취소 직전 상태가 결정한다. 상태를 바꾸기 전에 읽어 둔다.
		//    (cancel() 이 HOLD/CONFIRMED 외 상태는 예외로 거부한다.)
		ReservationStatus prior = reservation.getStatus();

		// 위약금 판정 — 미결제(HOLD)는 0, 확정은 요금정책으로 계산한다. 재고 되돌리기 전에
		// 계산해 두어도 무방하다(순수 함수, 부작용 없음).
		CancellationCharge charge = (prior == ReservationStatus.HOLD)
				? CancellationCharge.unpaid()
				: CancellationPolicy.quote(reservation.getRatePlan(),
						reservation.getCheckInDate(), reservation.getTotalAmount(),
						LocalDate.now());

		LocalDate firstNight = reservation.getCheckInDate();
		LocalDate lastNight = reservation.getCheckOutDate().minusDays(1);

		// ★ 되돌릴 재고 행을 잠근다(D-018). 이 트랜잭션에서 재고 첫 조회 = 락 조회.
		Map<LocalDate, RoomInventory> inventoryByDate = new HashMap<>();
		for (RoomInventory inv : roomInventoryRepository.lockForUpdateNative(
				reservation.getRoomType().getId(), firstNight, lastNight)) {
			inventoryByDate.put(inv.getStayDate(), inv);
		}

		for (ReservationNight night : reservation.getNights()) {
			RoomInventory inv = inventoryByDate.get(night.getStayDate());
			if (inv == null) {
				// HOLD/CONFIRMED 인 예약의 재고 행이 사라지는 일은 없다(기준정보는 삭제 대신
				// 비활성, D-020). 방어적으로 건너뛰고 상태 전이는 진행한다.
				continue;
			}
			if (prior == ReservationStatus.HOLD) {
				inv.releaseHold(1);
			} else { // CONFIRMED — cancel() 가드가 그 외 상태를 이미 걸러 냈다.
				inv.releaseSold(1);
			}
		}

		reservation.cancel(reason, charge.penalty());
		log.info("예약 취소: no={} {}~{} 직전상태={} 사유={} 위약금={}({}) 환불={}",
				reservation.getReservationNo(), reservation.getCheckInDate(),
				reservation.getCheckOutDate(), prior, reason,
				charge.penalty(), charge.basis(), charge.refund());
		return charge;
	}
}
