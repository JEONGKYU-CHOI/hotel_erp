package io.github.jeongkyuchoi.hotel.erp.common.domain.payment;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제. 예약당 성공 결제 한 행이다(D-034).
 *
 * <p><b>이 행의 존재 자체가 "그 예약은 결제됐다"는 사실이다.</b> {@code payment_key} 에
 * 유니크 제약이 걸려 있어, 같은 결제 건을 두 번 기록하려 하면 두 번째 INSERT 가 DB 에서
 * 거부된다 — 결제 멱등성의 1차 방어선이다(스키마 {@code V3__payment.sql} 주석 참조).
 * 웹훅·승인 콜백 재시도가 예약을 이중 확정시키지 못하게 하는 근거다.
 *
 * <p>{@code amount} 는 승인 시점의 금액 스냅샷이다. 위변조 검증에서 예약의 총액·토스
 * 응답 금액과 대조하는 기준값이며, 한 번 기록되면 바뀌지 않는다. 감사 컬럼이 시각뿐이라
 * {@link BaseTimeEntity} 를 상속한다.
 */
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/** 확정 대상 예약 id. FK 로만 참조하고 연관 매핑은 두지 않는다(집계·조회 경계 단순화). */
	@Column(name = "reservation_id", nullable = false)
	private Long reservationId;

	/** 토스 orderId. 우리 예약번호(reservation_no)를 그대로 쓴다. 예약당 유일. */
	@Column(name = "order_id", nullable = false, length = 64)
	private String orderId;

	/** 토스가 결제 건마다 발급하는 유일 키. 멱등의 기준이자 유니크 키. */
	@Column(name = "payment_key", nullable = false, length = 200)
	private String paymentKey;

	/** 승인 금액 스냅샷. 예약 total_amount·토스 응답 금액과 대조한다. */
	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status;

	/** 결제수단(카드 등). 토스 응답 값. */
	@Column(name = "method", length = 30)
	private String method;

	/** 토스 승인 시각. */
	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Builder
	private Payment(Long tenantId, Long reservationId, String orderId, String paymentKey,
			BigDecimal amount, PaymentStatus status, String method, LocalDateTime approvedAt) {
		this.tenantId = tenantId;
		this.reservationId = reservationId;
		this.orderId = orderId;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.status = status;
		this.method = method;
		this.approvedAt = approvedAt;
	}
}
