package io.github.jeongkyuchoi.hotel.erp.payment.web;

import io.github.jeongkyuchoi.hotel.erp.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 REST — 승인 콜백과 토스 웹훅(D-034). 결제 성공이 HOLD → CONFIRMED 를 잇는다(D-030 §2).
 *
 * <p><b>인증 주체가 없는 경로다.</b> 승인은 결제창에서 받은 {@code paymentKey}·금액 대조·확정
 * 서비스 멱등으로, 웹훅은 멱등·금액 대조로 보호한다(SecurityConfig 의 {@code /api/payments/**}
 * permitAll). 예외 → HTTP 매핑은 booking 의 {@code ApiExceptionHandler} 가 담당한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentApiController {

	private final PaymentService paymentService;

	/**
	 * 결제 승인. 프론트가 결제창 성공 후 넘긴 값으로 토스 승인을 확정하고 예약을 CONFIRMED 로
	 * 전이한다. 금액 위변조·승인 거절·만료 뒤 도착은 각각 예외로 걸러진다.
	 */
	@PostMapping("/confirm")
	public PaymentConfirmResponse confirm(@Valid @RequestBody PaymentConfirmRequest request) {
		return PaymentConfirmResponse.from(paymentService.confirm(request.toCommand()));
	}

	/**
	 * 토스 웹훅 수신. 승인 완료 이벤트로 사후 정합을 맞춘다(멱등). 토스에는 항상 200 으로
	 * 응답한다 — 4xx/5xx 를 주면 토스가 재전송을 반복하기 때문이다. 처리 실패는 로그로 남긴다.
	 */
	@PostMapping("/webhook")
	public ResponseEntity<Void> webhook(@RequestBody TossWebhookRequest request) {
		if (request.data() != null) {
			try {
				paymentService.reconcileFromWebhook(
						request.data().paymentKey(),
						request.data().orderId(),
						request.data().totalAmount(),
						request.data().status());
			} catch (Exception e) {
				// 재전송 폭주를 막으려 200 으로 삼킨다. 정합 실패는 로그·후속 배치로 다룬다.
				log.error("웹훅 처리 실패 — orderId={}", request.data().orderId(), e);
			}
		}
		return ResponseEntity.ok().build();
	}
}
