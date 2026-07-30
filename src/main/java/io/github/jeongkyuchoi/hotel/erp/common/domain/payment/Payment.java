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

	/** 환불(취소) 누적액(D-039). 유효 결제액 = {@code amount - canceledAmount}. */
	@Column(name = "canceled_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal canceledAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status;

	/** 결제수단(카드 등). 토스 응답 값. */
	@Column(name = "method", length = 30)
	private String method;

	/** 토스 승인 시각. */
	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	/** 마지막 환불 실행 시각(D-039). 환불 이력이 없으면 null. */
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Builder
	private Payment(Long tenantId, Long reservationId, String orderId, String paymentKey,
			BigDecimal amount, PaymentStatus status, String method, LocalDateTime approvedAt) {
		this.tenantId = tenantId;
		this.reservationId = reservationId;
		this.orderId = orderId;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.canceledAmount = BigDecimal.ZERO;
		this.status = status;
		this.method = method;
		this.approvedAt = approvedAt;
	}

	/** 아직 환불되지 않은 유효 결제액 = {@code amount - canceledAmount}. 폴리오 대변의 근거. */
	public BigDecimal effectiveAmount() {
		return amount.subtract(canceledAmount);
	}

	/**
	 * 환불(부분취소)을 이 결제에 누적 반영한다(D-039). 토스 취소 성공 <b>후</b> 호출한다 —
	 * 이 메서드는 원장 상태만 바꾼다.
	 *
	 * <p>취소 누적액이 승인액에 도달하면 {@link PaymentStatus#CANCELED} 로 올린다(완전 취소).
	 * 그 전까지는 APPROVED(부분 취소)로 남는다.
	 *
	 * @throws IllegalArgumentException 취소액이 0 이하이거나 남은 유효 결제액을 넘으면.
	 */
	public void applyCancel(BigDecimal cancelAmount, LocalDateTime at) {
		if (cancelAmount == null || cancelAmount.signum() <= 0) {
			throw new IllegalArgumentException("취소액은 0보다 커야 합니다. amount=" + cancelAmount);
		}
		if (cancelAmount.compareTo(effectiveAmount()) > 0) {
			throw new IllegalArgumentException(
					"취소액이 남은 결제액을 넘습니다. 취소=" + cancelAmount + " 남은=" + effectiveAmount());
		}
		this.canceledAmount = this.canceledAmount.add(cancelAmount);
		this.canceledAt = at;
		if (this.canceledAmount.compareTo(this.amount) == 0) {
			this.status = PaymentStatus.CANCELED;
		}
	}
}
