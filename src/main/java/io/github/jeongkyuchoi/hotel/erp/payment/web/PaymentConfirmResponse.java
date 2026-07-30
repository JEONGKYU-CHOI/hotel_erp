package io.github.jeongkyuchoi.hotel.erp.payment.web;

import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.Payment;
import java.math.BigDecimal;

/**
 * 결제 승인 결과 응답(D-034). 확정된 예약번호와 결제 상태를 프론트에 돌려준다.
 *
 * @param orderId    주문번호(=예약번호). 프론트는 이걸로 예약 조회 화면으로 넘어간다
 * @param paymentKey 토스 결제 키
 * @param amount     승인 금액
 * @param status     결제 상태(APPROVED)
 */
public record PaymentConfirmResponse(
		String orderId,
		String paymentKey,
		BigDecimal amount,
		String status) {

	public static PaymentConfirmResponse from(Payment payment) {
		return new PaymentConfirmResponse(
				payment.getOrderId(),
				payment.getPaymentKey(),
				payment.getAmount(),
				payment.getStatus().name());
	}
}
