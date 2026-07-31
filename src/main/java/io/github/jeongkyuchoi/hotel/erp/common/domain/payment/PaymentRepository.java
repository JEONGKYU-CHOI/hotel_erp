package io.github.jeongkyuchoi.hotel.erp.common.domain.payment;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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

	/**
	 * 예약 한 건의 결제 행을 {@code FOR UPDATE} 로 잠그고 승인 시각 순으로 돌려준다(환불 직렬화,
	 * D-039). 취소·확정이 예약 행 락으로 전이를 직렬화하듯(D-025), 환불은 결제 행 락을
	 * <b>직렬화 지점</b>으로 삼는다. 동시 환불 요청 둘이 같은 예약을 두고 경합해도, 먼저 잠근
	 * 트랜잭션이 취소액을 반영하고 커밋하면, 뒤 트랜잭션은 갱신된 {@code effectiveAmount} 를
	 * 읽어 폴리오 잔액이 0(이상)임을 보고 무동작한다 — 이중 환불(같은 결제를 두 번 취소)이 막힌다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.reservationId = :reservationId order by p.approvedAt")
	List<Payment> findByReservationIdForUpdateOrderByApprovedAt(Long reservationId);
}
