package io.github.jeongkyuchoi.hotel.erp.notification.event;

/**
 * 임시 예약(HOLD)이 만들어졌을 때. 결제 안내 메일의 트리거다.
 * 트랜잭션이 커밋된 뒤에만 처리된다(AFTER_COMMIT) — 롤백된 예약엔 메일이 나가지 않는다.
 */
public record ReservationHeldEvent(Long reservationId) {
}
