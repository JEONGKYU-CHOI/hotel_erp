package io.github.jeongkyuchoi.hotel.erp.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * 토스 승인 API({@code POST /v1/payments/confirm}) 성공 응답 중 우리가 쓰는 필드만 담는다.
 *
 * <p>토스 응답은 필드가 수십 개다 — {@code @JsonIgnoreProperties(ignoreUnknown = true)} 로
 * 모르는 필드는 무시한다. 그래야 토스가 필드를 늘려도 역직렬화가 깨지지 않는다.
 *
 * @param paymentKey  결제 건 유일 키. 우리가 보낸 값과 같아야 한다.
 * @param orderId     주문번호. 우리 예약번호와 같아야 한다.
 * @param totalAmount 승인된 총 금액. 예약 total_amount 와 대조하는 최종 근거값.
 * @param status      토스 결제 상태. 승인 성공이면 {@code DONE}.
 * @param method      결제수단(카드 등).
 * @param approvedAt  승인 시각(ISO-8601, 오프셋 포함 문자열).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
		String paymentKey,
		String orderId,
		BigDecimal totalAmount,
		String status,
		String method,
		String approvedAt) {
}
