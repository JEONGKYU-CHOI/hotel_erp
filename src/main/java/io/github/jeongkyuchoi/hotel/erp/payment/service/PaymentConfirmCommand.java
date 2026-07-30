package io.github.jeongkyuchoi.hotel.erp.payment.service;

import java.math.BigDecimal;

/**
 * 결제 승인 커맨드 (D-034). 프론트가 결제창에서 받아 넘기는 값이다.
 *
 * @param paymentKey 토스가 발급한 결제 건 키
 * @param orderId    주문번호(우리 예약번호). 어느 예약의 결제인지 식별한다
 * @param amount     프론트가 보낸 결제 금액. <b>믿지 않고</b> 서버 저장액과 대조하는 대상이다
 */
public record PaymentConfirmCommand(String paymentKey, String orderId, BigDecimal amount) {
}
