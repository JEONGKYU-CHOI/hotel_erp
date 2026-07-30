package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import java.math.BigDecimal;

/**
 * 취소 과금 판정 결과(D-037) — 얼마를 위약금으로 물리고 얼마를 환불하는지.
 *
 * <p>{@link CancellationPolicy} 가 계산해 돌려주는 순수 값이다. {@code penalty + refund} 는
 * 언제나 결제 총액과 같다(무결성). 미결제(HOLD) 취소는 둘 다 0 이다 — 돌려줄 결제가 없다.
 *
 * @param penalty 위약금(원 단위로 반올림된 금액). 예약 행에 스냅샷으로 저장된다.
 * @param refund  환불액 = 총액 − 위약금.
 * @param basis   판정 근거. 사용자 안내·감사(audit)에 쓴다.
 */
public record CancellationCharge(BigDecimal penalty, BigDecimal refund, Basis basis) {

	/** 위약금 판정의 근거. */
	public enum Basis {
		/** 미결제(HOLD) 취소 — 돌려줄 결제가 없어 위약금·환불 모두 0. */
		UNPAID,
		/** 무료 취소 기한 이내 — 위약금 0, 전액 환불. */
		FREE,
		/** 무료 취소 기한 경과 — penalty_rate 만큼 위약금. */
		DEADLINE_PASSED,
		/** 환불 불가 요금정책 — 위약금 100%, 환불 0. */
		NON_REFUNDABLE,
		/** 이미 취소된 예약의 재조회(멱등) — 저장된 스냅샷을 그대로 돌려준다. */
		SETTLED
	}

	/** 미결제(HOLD) 취소 — 위약금·환불 모두 0. */
	public static CancellationCharge unpaid() {
		return new CancellationCharge(BigDecimal.ZERO, BigDecimal.ZERO, Basis.UNPAID);
	}

	/**
	 * 이미 취소된 예약의 스냅샷을 재구성한다(멱등 경로). 저장된 위약금이 없으면(과거 데이터)
	 * 0 으로 본다.
	 */
	public static CancellationCharge settled(BigDecimal storedFee, BigDecimal totalAmount) {
		BigDecimal fee = storedFee != null ? storedFee : BigDecimal.ZERO;
		return new CancellationCharge(fee, totalAmount.subtract(fee), Basis.SETTLED);
	}
}
