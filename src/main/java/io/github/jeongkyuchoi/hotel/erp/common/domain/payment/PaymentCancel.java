package io.github.jeongkyuchoi.hotel.erp.common.domain.payment;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불(부분취소) 이벤트 원장 (D-042). 환불 실행 한 번마다 한 행이 쌓이는 append-only 기록이다.
 *
 * <p><b>왜 별도 원장인가.</b> {@link Payment#getCanceledAmount()} 는 취소 "누적 총액"만 남겨
 * 잔액 계산·멱등엔 충분하지만(D-039), 부분환불을 여러 번 하면 "누가·언제·얼마·왜"가 총액에
 * 뭉개진다. 이 원장은 각 환불을 개별 이벤트로 보존한다 — payment.canceled_amount 는 이 원장
 * {@code cancelAmount} 합과 일치한다.
 *
 * <p><b>행위자·시각은 감사가 채운다.</b> {@link BaseEntity} 를 상속해 {@code createdBy}(로그인
 * 직원명 또는 배치의 SYSTEM)·{@code createdAt} 이 JPA 감사(AuditorAware)로 자동 기록된다.
 * append-only 라 정정하지 않는다 — 잘못된 환불은 반대 이벤트가 아니라 원장 자체로 추적된다.
 */
@Entity
@Table(name = "payment_cancel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCancel extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/** 환불 대상 결제 id. FK 로만 참조하고 연관 매핑은 두지 않는다(집계·조회 경계 단순화). */
	@Column(name = "payment_id", nullable = false)
	private Long paymentId;

	/** 조회 편의용 비정규화 — 예약별 환불 이력을 결제 조인 없이 뽑는다. */
	@Column(name = "reservation_id", nullable = false)
	private Long reservationId;

	/** 이 이벤트에서 환불(취소)한 금액. 0 보다 크다. */
	@Column(name = "cancel_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal cancelAmount;

	/** 환불 사유. */
	@Column(name = "reason", length = 200)
	private String reason;

	@Builder
	private PaymentCancel(Long tenantId, Long paymentId, Long reservationId,
			BigDecimal cancelAmount, String reason) {
		this.tenantId = tenantId;
		this.paymentId = paymentId;
		this.reservationId = reservationId;
		this.cancelAmount = cancelAmount;
		this.reason = reason;
	}
}
