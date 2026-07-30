package io.github.jeongkyuchoi.hotel.erp.payment.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * 토스 결제 웹훅 본문(D-034). 우리가 쓰는 필드만 담고 나머지는 무시한다.
 *
 * <p>토스는 결제 상태가 바뀔 때 {@code eventType} 과 결제 객체({@code data})를 보낸다.
 * 승인 완료면 {@code data.status == "DONE"}. 정합에 필요한 건 결제 키·주문번호·상태·금액뿐이다.
 *
 * @param eventType 이벤트 종류(예: {@code PAYMENT_STATUS_CHANGED})
 * @param data      결제 객체
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossWebhookRequest(String eventType, Data data) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Data(String paymentKey, String orderId, String status, BigDecimal totalAmount) {
	}
}
