package io.github.jeongkyuchoi.hotel.erp.booking.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HOLD 생성 응답(D-030).
 *
 * <p>{@code holdExpiresAt} 까지 결제하지 않으면 점유가 만료된다(D-003). 부킹엔진은 이 시각을
 * 받아 결제 페이지에 카운트다운을 띄운다. 예약번호는 결제 단계로 넘겨 결제 성공 시 확정에 쓴다.
 */
public record HoldResponse(
		String reservationNo,
		String status,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		int nightCount,
		BigDecimal totalAmount,
		LocalDateTime holdExpiresAt) {

	public static HoldResponse from(Reservation r) {
		return new HoldResponse(
				r.getReservationNo(),
				r.getStatus().name(),
				r.getCheckInDate(),
				r.getCheckOutDate(),
				r.nights(),
				r.getTotalAmount(),
				r.getHoldExpiresAt());
	}
}
