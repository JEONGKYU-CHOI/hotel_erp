package io.github.jeongkyuchoi.hotel.erp.common.domain.payment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 환불 이벤트 원장 조회 (D-042). 예약별 환불 이력을 시각 순으로 돌려준다 — 폴리오·백오피스에서
 * "이 예약에 언제·누가·얼마 환불했는지"를 보여줄 때 쓴다.
 */
public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {

	List<PaymentCancel> findByReservationIdOrderByCreatedAt(Long reservationId);
}
