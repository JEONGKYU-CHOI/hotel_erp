package io.github.jeongkyuchoi.hotel.erp.payment.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.Payment;
import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.PaymentRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.PaymentStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.common.exception.PaymentException;
import io.github.jeongkyuchoi.hotel.erp.payment.client.TossConfirmResponse;
import io.github.jeongkyuchoi.hotel.erp.payment.client.TossPaymentClient;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 승인 → 예약 확정 (D-034). 결제 성공이 HOLD → CONFIRMED 의 유일한 트리거다(D-030 §2).
 *
 * <p><b>금액 위변조를 두 지점에서 막는다.</b> 프론트가 보낸 금액은 신뢰하지 않는다.
 * ① 승인 요청 전, 프론트가 보낸 {@code amount} 를 예약의 {@code totalAmount}(서버 저장값)와
 * 대조한다 — 결제창에서 금액을 조작해 싸게 결제하려는 시도를 막는다. ② 토스 승인 응답의
 * {@code totalAmount} 를 다시 예약 저장값과 대조한다 — 응답 위조·중간 변조를 막는다. 시크릿
 * 키는 서버에만 있으므로(클라이언트로 안 나감) 이 대조가 성립한다.
 *
 * <p><b>멱등.</b> 웹훅·승인 콜백은 재시도된다. 같은 {@code paymentKey} 로 이미 기록된 결제가
 * 있으면 토스를 다시 부르지 않고 그 결제를 그대로 돌려준다(선조회 멱등). 경합으로 선조회를
 * 놓쳐도 {@code uk_payment_key} 유니크 제약이 최후에 막고, 확정 서비스 자체도 멱등이다.
 *
 * <p><b>트랜잭션 경계.</b> 확정(재고 이동·상태 전이)과 결제 기록을 한 트랜잭션에 묶어 둘이
 * 함께 커밋되거나 함께 롤백되게 한다. 토스 승인(네트워크)은 그 앞에서 일어나며, 승인 성공
 * 후 DB 커밋이 실패하는 드문 창은 결제 웹훅이 사후 정합을 맞춘다(1차 범위의 감수 대가).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private static final Long TENANT_ID = 1L;

	private final ReservationRepository reservationRepository;
	private final PaymentRepository paymentRepository;
	private final ReservationConfirmService reservationConfirmService;
	private final TossPaymentClient tossPaymentClient;

	@Transactional
	public Payment confirm(PaymentConfirmCommand command) {
		// ① 선조회 멱등 — 이미 처리된 결제면 토스를 다시 부르지 않는다.
		Payment existing = paymentRepository.findByPaymentKey(command.paymentKey()).orElse(null);
		if (existing != null) {
			log.info("결제 재요청(멱등) — paymentKey={} orderId={}",
					command.paymentKey(), existing.getOrderId());
			return existing;
		}

		// ② 예약 조회 (orderId = 예약번호).
		Reservation reservation = reservationRepository
				.findByTenantIdAndReservationNo(TENANT_ID, command.orderId())
				.orElseThrow(() -> new NotFoundException(
						"주문에 해당하는 예약을 찾을 수 없습니다. orderId=" + command.orderId()));

		BigDecimal expected = reservation.getTotalAmount();

		// ③ 위변조 검증 1 — 프론트가 보낸 금액 vs 서버 저장액.
		if (expected.compareTo(command.amount()) != 0) {
			throw new PaymentException("AMOUNT_MISMATCH",
					"요청 금액이 예약 금액과 다릅니다. orderId=" + command.orderId());
		}

		// ④ 토스 승인. 카드 거절·이미 처리됨 등은 PaymentException 으로 던져진다.
		TossConfirmResponse response = tossPaymentClient.confirm(
				command.paymentKey(), command.orderId(), command.amount());

		// ⑤ 위변조 검증 2 — 토스 승인 응답 금액 vs 서버 저장액 + 키 일치.
		if (expected.compareTo(response.totalAmount()) != 0) {
			throw new PaymentException("AMOUNT_MISMATCH",
					"승인 금액이 예약 금액과 다릅니다. orderId=" + command.orderId());
		}
		if (!command.paymentKey().equals(response.paymentKey())
				|| !command.orderId().equals(response.orderId())) {
			throw new PaymentException("RESPONSE_MISMATCH",
					"승인 응답의 주문/결제 식별자가 요청과 다릅니다. orderId=" + command.orderId());
		}

		// ⑥⑦ 확정 배선 + 결제 기록.
		Payment payment = persistApproved(reservation, command.paymentKey(), command.orderId(),
				expected, response.method(), parseApprovedAt(response.approvedAt()));

		log.info("결제 승인·확정 완료 — orderId={} paymentKey={} amount={}",
				command.orderId(), command.paymentKey(), expected);
		return payment;
	}

	/**
	 * 토스 웹훅 기반 사후 정합 (D-034, 재조회 검증 D-041). successUrl 콜백이 커밋 전에 끊긴
	 * 드문 창을 메운다.
	 *
	 * <p><b>웹훅 본문은 믿지 않는다(재조회 검증).</b> {@code /api/payments/webhook} 은 인증 주체가
	 * 없는 공개 경로라, 위조자가 "결제 완료" 이벤트를 직접 보낼 수 있다. 그래서 본문의 금액·상태·
	 * 주문번호는 신뢰하지 않고, 본문의 {@code paymentKey} 로 토스에 결제를 <b>다시 조회</b>해
	 * (시크릿 키 인증) 권위 응답으로 정합한다. 위조 웹훅은 실재하지 않는 키라 조회에서 걸러지고,
	 * 조작된 금액·상태는 권위 응답에 없어 오확정되지 않는다. 본문 {@code status} 는 값싼 사전
	 * 필터로만 쓴다(불필요한 조회 API 호출 절약).
	 *
	 * <p><b>멱등.</b> 웹훅은 재시도·중복 전송된다. 이미 기록된 결제면 아무 것도 하지 않는다.
	 * 경합으로 선조회를 놓쳐도 {@code uk_payment_key} 와 확정 서비스 멱등이 최후에 막는다.
	 */
	@Transactional
	public void reconcileFromWebhook(String paymentKey, String status) {
		if (!"DONE".equals(status)) {
			return; // 본문 상태는 값싼 사전 필터. 권위 판정은 아래 재조회로 한다.
		}
		if (paymentKey == null || paymentKey.isBlank()) {
			log.warn("웹훅 정합 — paymentKey 없음, 무시.");
			return;
		}
		if (paymentRepository.findByPaymentKey(paymentKey).isPresent()) {
			return; // 이미 처리됨(멱등). 대개 successUrl 콜백이 먼저 끝낸 경우다.
		}

		// ★ 재조회 검증(D-041). 본문 대신 토스에 직접 물어 권위 응답을 받는다. 위조 웹훅은
		//    조회 실패(존재하지 않는 키)로 걸러진다.
		TossConfirmResponse actual;
		try {
			actual = tossPaymentClient.getPayment(paymentKey);
		} catch (PaymentException e) {
			log.warn("웹훅 정합 — 토스 결제 조회 실패(위조 의심 포함), 무시. paymentKey={} code={}",
					paymentKey, e.getCode());
			return;
		}
		if (!"DONE".equals(actual.status())) {
			log.warn("웹훅 정합 — 토스 실제 상태가 DONE 아님, 확정 보류. paymentKey={} status={}",
					paymentKey, actual.status());
			return;
		}

		Reservation reservation = reservationRepository
				.findByTenantIdAndReservationNo(TENANT_ID, actual.orderId()).orElse(null);
		if (reservation == null) {
			log.warn("웹훅 정합 — 예약 없음, 무시. orderId={}", actual.orderId());
			return;
		}
		if (actual.totalAmount() == null
				|| reservation.getTotalAmount().compareTo(actual.totalAmount()) != 0) {
			log.warn("웹훅 정합 — 금액 불일치, 확정 보류. orderId={} tossAmount={}",
					actual.orderId(), actual.totalAmount());
			return;
		}
		persistApproved(reservation, paymentKey, actual.orderId(), reservation.getTotalAmount(),
				actual.method(), parseApprovedAt(actual.approvedAt()));
		log.info("웹훅 재조회 정합으로 확정 완료 — orderId={} paymentKey={}", actual.orderId(), paymentKey);
	}

	/** 확정 배선(HOLD → CONFIRMED) + 결제 기록. 만료 뒤 도착이면 확정 서비스 가드가 거부한다. */
	private Payment persistApproved(Reservation reservation, String paymentKey, String orderId,
			BigDecimal amount, String method, LocalDateTime approvedAt) {
		reservationConfirmService.confirm(reservation.getId());
		return paymentRepository.save(Payment.builder()
				.tenantId(TENANT_ID)
				.reservationId(reservation.getId())
				.orderId(orderId)
				.paymentKey(paymentKey)
				.amount(amount)
				.status(PaymentStatus.APPROVED)
				.method(method)
				.approvedAt(approvedAt)
				.build());
	}

	/** 토스 승인 시각(ISO-8601 오프셋 문자열)을 LocalDateTime 으로. 형식이 없거나 어긋나면 null. */
	private LocalDateTime parseApprovedAt(String approvedAt) {
		if (approvedAt == null || approvedAt.isBlank()) {
			return null;
		}
		try {
			return OffsetDateTime.parse(approvedAt).toLocalDateTime();
		} catch (Exception e) {
			log.warn("승인 시각 파싱 실패 — 무시하고 진행. value={}", approvedAt);
			return null;
		}
	}
}
