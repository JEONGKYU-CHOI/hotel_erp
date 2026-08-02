package io.github.jeongkyuchoi.hotel.erp.reservation.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회원 예약 목록의 한 줄 요약(D-032). {@code GET /api/me/reservations} 가 돌려준다.
 *
 * <p>목록이라 상세(연락처·야간별·취소사유 등)는 담지 않는다 — 상세는 예약 조회(D-028)가
 * 맡는다. {@code status} 는 표시용 상태다: HOLD 인데 만료시각이 지났으면 스케줄러가 아직
 * 정리하지 못했어도 {@link ReservationStatus#EXPIRED} 로 보여준다(D-003, D-028 과 같은 규율).
 */
public record MyReservationSummary(
		String reservationNo,
		ReservationStatus status,
		String roomTypeName,
		String ratePlanName,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		int nights,
		BigDecimal totalAmount,
		LocalDateTime holdExpiresAt) {

	public static MyReservationSummary from(Reservation r, LocalDateTime now) {
		return new MyReservationSummary(
				r.getReservationNo(),
				r.displayStatus(now), // 만료 지난 HOLD 표시 규칙은 도메인에 모았다(D-043)
				r.getRoomType().getName(),
				r.getRatePlan().getName(),
				r.getCheckInDate(),
				r.getCheckOutDate(),
				r.nights(),
				r.getTotalAmount(),
				r.getHoldExpiresAt()); // 목록에서도 HOLD 결제 마감 카운트다운을 띄운다
	}
}
