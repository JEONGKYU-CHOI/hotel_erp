package io.github.jeongkyuchoi.hotel.erp.notification.event;

/**
 * 예약이 확정(결제 완료)됐을 때. 예약 확정 안내 메일의 트리거다.
 * 확정이 실제로 일어난 경우에만 발행한다(멱등 재확정에선 발행하지 않아 메일 중복을 막는다).
 */
public record ReservationConfirmedEvent(Long reservationId) {
}
