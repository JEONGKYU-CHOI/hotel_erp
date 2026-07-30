package io.github.jeongkyuchoi.hotel.erp.common.domain.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결제 조회.
 *
 * <p>결제 서비스는 승인 전에 {@link #findByPaymentKey} 로 이미 기록된 결제 건인지 먼저
 * 확인한다(선조회 멱등). 웹훅·승인 콜백 재시도가 이 조회에서 걸러지면 토스 승인 API 를
 * 다시 부르지 않는다. 유니크 제약 {@code uk_payment_key} 가 최후에 막지만, 예외 전에
 * 조회로 거르는 편이 재시도 API 로서 깔끔하다(D-026 과 같은 규율).
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByPaymentKey(String paymentKey);

	/** 예약 한 건의 결제 내역(폴리오 조립용, D-038). 승인 시각 순으로 원장 라인을 만든다. */
	List<Payment> findByReservationIdOrderByApprovedAt(Long reservationId);
}
