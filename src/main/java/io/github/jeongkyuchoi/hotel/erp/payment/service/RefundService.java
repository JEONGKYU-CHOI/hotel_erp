package io.github.jeongkyuchoi.hotel.erp.payment.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.Payment;
import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.PaymentRepository;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse;
import io.github.jeongkyuchoi.hotel.erp.folio.service.FolioService;
import io.github.jeongkyuchoi.hotel.erp.payment.client.TossPaymentClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환불 실행 (D-039). 폴리오 잔액이 환불 대상(REFUND_DUE)인 예약에 대해, 돌려줄 금액만큼
 * 토스 결제취소를 실행하고 원장(payment.canceled_amount)에 반영한다.
 *
 * <p><b>환불액은 폴리오가 정한다.</b> 돌려줄 금액 = {@code -balance}(= 유효 결제액 − 청구액).
 * 위약금(D-037)이 있으면 그만큼 남기는 부분환불이 된다. 청구가 결제와 같거나 크면(완납·미수)
 * 돌려줄 것이 없어 아무 것도 하지 않는다.
 *
 * <p><b>토스 성공 후 원장 반영.</b> 결제취소 API 가 성공한 뒤에만 {@code canceled_amount} 를
 * 올린다. 토스가 실패하면 {@link io.github.jeongkyuchoi.hotel.erp.common.exception.PaymentException}
 * 이 올라와 트랜잭션이 롤백되고 원장도 그대로다 — 실제 돈과 장부가 어긋나지 않는다.
 *
 * <p><b>멱등.</b> 환불 후 잔액은 0 이 되므로, 재실행하면 "돌려줄 것 없음"으로 무동작한다.
 * 이미 환불된 만큼은 {@code effectiveAmount} 가 줄어 다시 취소 대상이 되지 않는다.
 *
 * <p><b>동시성.</b> 폴리오·결제를 읽기 <b>전에</b> 결제 행을 {@code FOR UPDATE} 로 잠근다(D-025
 * 와 같은 규율 — 전이 전 잠그기). 락 없이 읽으면 동시 환불 둘이 같은 {@code balance<0} 을 보고
 * 둘 다 토스 취소를 불러 이중 환불이 날 수 있다(멱등은 순차 재요청만 막는다). 결제 행을 먼저
 * 잠그면 뒤 트랜잭션은 앞이 커밋한 갱신된 유효액을 읽어 무동작한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

	private final FolioService folioService;
	private final PaymentRepository paymentRepository;
	private final TossPaymentClient tossPaymentClient;

	/**
	 * 예약의 환불 대상 금액을 토스로 취소하고 원장에 반영한다. 갱신된 청구서를 돌려준다.
	 *
	 * @return 환불 반영 후의 폴리오(잔액 0 또는 미수/완납이면 무동작 후 그대로)
	 */
	@Transactional
	public FolioResponse refund(Long reservationId, String reason) {
		// ★ 결제 행을 먼저 잠근다(D-025 규율). 폴리오 잔액 판정과 취소 실행 사이에 다른 환불이
		//    끼어들지 못하게 하는 직렬화 지점 — 동시 이중 환불을 막는다. 같은 트랜잭션에서 뒤이어
		//    folioService 가 결제를 다시 읽어도 방금 잠근 최신 유효액을 본다.
		List<Payment> payments =
				paymentRepository.findByReservationIdForUpdateOrderByApprovedAt(reservationId);

		FolioResponse folio = folioService.forAdmin(reservationId);

		// 돌려줄 것이 없다 — 미수(>0)·완납(0) 이면 무동작(멱등).
		if (folio.balance().signum() >= 0) {
			log.info("환불 요청 무동작 — 돌려줄 잔액 없음. reservationId={} balance={}",
					reservationId, folio.balance());
			return folio;
		}

		BigDecimal refundDue = folio.balance().negate();
		BigDecimal left = refundDue;

		// 결제들의 남은 유효액에서 순서대로 환불한다(보통 예약당 승인 결제 1건). 위에서 잠근 행을
		// 재사용한다.
		for (Payment p : payments) {
			if (left.signum() == 0) {
				break;
			}
			BigDecimal remaining = p.effectiveAmount();
			if (remaining.signum() <= 0) {
				continue;
			}
			BigDecimal cancelAmount = remaining.min(left);
			// ★ 토스 취소 먼저 — 성공해야 원장을 바꾼다. 실패 시 예외로 롤백.
			tossPaymentClient.cancel(p.getPaymentKey(), cancelAmount, reason);
			p.applyCancel(cancelAmount, LocalDateTime.now());
			left = left.subtract(cancelAmount);
			log.info("환불 실행 — reservationId={} paymentKey={} 취소={} 남은유효={}",
					reservationId, p.getPaymentKey(), cancelAmount, p.effectiveAmount());
		}

		if (left.signum() > 0) {
			// 결제 유효액 합보다 돌려줄 금액이 크다 — 데이터 정합이 깨진 상황이라 롤백한다.
			throw new IllegalStateException(
					"환불 대상 금액을 결제에서 모두 취소하지 못했습니다. 남은=" + left);
		}

		return folioService.forAdmin(reservationId);
	}
}
