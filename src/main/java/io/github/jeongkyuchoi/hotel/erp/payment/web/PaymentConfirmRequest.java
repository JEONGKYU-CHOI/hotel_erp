package io.github.jeongkyuchoi.hotel.erp.payment.web;

import io.github.jeongkyuchoi.hotel.erp.payment.service.PaymentConfirmCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 결제 승인 요청 본문(D-034). 프론트가 토스 결제창에서 받은 값을 그대로 보낸다.
 *
 * <p>형식만 여기서 막고(빈 값·음수), 금액 위변조 검증은 서비스가 서버 저장값과 대조해서 한다 —
 * {@code amount} 가 유효한 숫자여도 예약 금액과 다르면 거부된다.
 */
public record PaymentConfirmRequest(
		@NotBlank String paymentKey,
		@NotBlank String orderId,
		@NotNull @Positive BigDecimal amount) {

	public PaymentConfirmCommand toCommand() {
		return new PaymentConfirmCommand(paymentKey, orderId, amount);
	}
}
