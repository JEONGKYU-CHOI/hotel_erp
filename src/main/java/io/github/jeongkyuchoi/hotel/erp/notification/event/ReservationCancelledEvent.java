package io.github.jeongkyuchoi.hotel.erp.notification.event;

import java.math.BigDecimal;

/**
 * 예약이 취소됐을 때. 취소·환불 안내 메일의 트리거다.
 *
 * <p>위약금·환불액은 취소 판정({@code CancellationCharge})이 계산한 값을 그대로 싣는다 —
 * 예약 스냅샷의 {@code total - fee} 로 재계산하지 않는다. 미결제(HOLD) 취소는 결제가 없어
 * 환불이 0 인데, 예약 총액에서 빼면 잘못된 환불액이 나오기 때문이다.
 *
 * <p>실제 상태 전이가 일어난 경우에만 발행한다(멱등 재취소에선 발행하지 않는다).
 */
public record ReservationCancelledEvent(Long reservationId, BigDecimal penalty, BigDecimal refund) {
}
