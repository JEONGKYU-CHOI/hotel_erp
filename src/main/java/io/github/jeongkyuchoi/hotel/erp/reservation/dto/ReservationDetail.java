package io.github.jeongkyuchoi.hotel.erp.reservation.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 상세 조회 결과(D-028). 비회원 조회(예약번호 + 전화)가 돌려주는 읽기 뷰다.
 *
 * <p><b>엔티티를 그대로 내보내지 않는다.</b> {@link Reservation} 은 지연로딩 연관
 * (roomType·ratePlan·nights)을 갖고 있어, 트랜잭션 밖에서 접근하면 {@code
 * LazyInitializationException} 이 난다. 조립을 읽기 전용 트랜잭션 안에서 끝내고 이 값 객체로
 * 넘긴다.
 *
 * <p><b>{@code status} 는 표시용 상태다.</b> HOLD 인데 {@code holdExpiresAt} 이 지났으면
 * 스케줄러가 아직 정리하지 못했어도 {@link ReservationStatus#EXPIRED} 로 보여준다 — 조회
 * 시점에 만료분을 만료로 취급해 스케줄러 주기 지연을 사용자에게 감춘다(D-003).
 */
public record ReservationDetail(
		String reservationNo,
		ReservationStatus status,
		String guestName,
		String guestPhone,
		String guestEmail,
		String roomTypeCode,
		String roomTypeName,
		String ratePlanName,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		int nights,
		int adults,
		int children,
		BigDecimal totalAmount,
		LocalDateTime holdExpiresAt,
		LocalDateTime cancelledAt,
		String cancelReason,
		String assignedRoomNo,
		List<ReservationNightView> nightViews) {

	/**
	 * 엔티티에서 상세 뷰를 조립한다. <b>반드시 읽기 전용 트랜잭션 안에서 부른다</b> —
	 * 지연로딩 연관에 접근하기 때문이다.
	 *
	 * @param now 만료 판정 기준 시각(D-003)
	 */
	public static ReservationDetail from(Reservation r, LocalDateTime now) {
		ReservationStatus displayStatus =
				r.isHoldExpired(now) ? ReservationStatus.EXPIRED : r.getStatus();

		List<ReservationNightView> views = r.getNights().stream()
				.sorted((a, b) -> a.getStayDate().compareTo(b.getStayDate()))
				.map(ReservationNightView::from)
				.toList();

		return new ReservationDetail(
				r.getReservationNo(),
				displayStatus,
				r.getGuestName(),
				r.getGuestPhone(),
				r.getGuestEmail(),
				r.getRoomType().getCode(),
				r.getRoomType().getName(),
				r.getRatePlan().getName(),
				r.getCheckInDate(),
				r.getCheckOutDate(),
				r.nights(),
				r.getAdults(),
				r.getChildren(),
				r.getTotalAmount(),
				r.getHoldExpiresAt(),
				r.getCancelledAt(),
				r.getCancelReason(),
				r.getRoom() != null ? r.getRoom().getRoomNo() : null,
				views);
	}
}
