package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 백오피스 예약 목록의 한 행(D-029).
 *
 * <p>엔티티를 그대로 뷰에 넘기지 않는다 — OSIV 를 껐으므로 렌더링 시점의 지연로딩 접근이
 * 예외가 된다. 목록에 필요한 값만 트랜잭션 안에서 뽑아 담는다.
 *
 * <p>{@code status} 는 표시용이다. 만료 시각이 지난 HOLD 는 스케줄러 정리 전이라도
 * EXPIRED 로 보여준다(D-003).
 *
 * @param status 표시용 상태(조회 시점 만료 반영)
 */
public record ReservationListRow(
		Long id,
		String reservationNo,
		String guestName,
		String guestPhone,
		String roomTypeName,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		int nights,
		ReservationStatus status,
		BigDecimal totalAmount) {

	public static ReservationListRow from(Reservation r, LocalDateTime now) {
		ReservationStatus displayStatus =
				r.isHoldExpired(now) ? ReservationStatus.EXPIRED : r.getStatus();
		return new ReservationListRow(
				r.getId(),
				r.getReservationNo(),
				r.getGuestName(),
				r.getGuestPhone(),
				r.getRoomType().getName(),
				r.getCheckInDate(),
				r.getCheckOutDate(),
				r.nights(),
				displayStatus,
				r.getTotalAmount());
	}
}
