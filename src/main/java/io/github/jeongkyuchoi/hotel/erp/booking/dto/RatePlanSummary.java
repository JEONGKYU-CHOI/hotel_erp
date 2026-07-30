package io.github.jeongkyuchoi.hotel.erp.booking.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import java.math.BigDecimal;

/**
 * 부킹엔진 노출용 요금정책 요약(D-030). 고객이 같은 객실에서 조식/환불 조건을 고르는 정보다.
 *
 * <p>{@code baseAmount} 는 폴백 기본요금이다 — 실제 청구 금액은 일자별 요금달력 반영 후
 * HOLD 응답의 {@code totalAmount} 로 확정된다. 여기서는 정책 비교용 표시값이다.
 *
 * @param id                요금정책 식별자. HOLD 요청에 싣는다
 * @param code              정책 코드
 * @param name              고객 노출 이름
 * @param baseAmount        1박 기본요금(표시용)
 * @param breakfastIncluded 조식 포함 여부
 * @param refundable        환불 가능 여부
 * @param cancelDeadlineDays 무료 취소 기한(체크인 N일 전)
 */
public record RatePlanSummary(
		Long id,
		String code,
		String name,
		BigDecimal baseAmount,
		boolean breakfastIncluded,
		boolean refundable,
		short cancelDeadlineDays) {

	public static RatePlanSummary from(RatePlan rp) {
		return new RatePlanSummary(
				rp.getId(), rp.getCode(), rp.getName(), rp.getBaseAmount(),
				rp.isBreakfastIncluded(), rp.isRefundable(), rp.getCancelDeadlineDays());
	}
}
