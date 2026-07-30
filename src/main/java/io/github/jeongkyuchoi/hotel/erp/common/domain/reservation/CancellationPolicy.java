package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 취소 위약금 계산(D-037) — 순수 함수. DB·시계에 의존하지 않아 단위 테스트로 전 경계를 덮는다.
 *
 * <p><b>규칙</b> (요금정책 필드로만 결정한다):
 * <ol>
 *   <li><b>환불 불가 정책</b>({@code refundable=false}) → 위약금 100%, 환불 0. 기한과 무관하다.</li>
 *   <li><b>무료 취소 기한 이내</b> — 취소일이 {@code 체크인 − cancel_deadline_days} 이하면
 *       위약금 0, 전액 환불.</li>
 *   <li><b>기한 경과</b> — 위약금 = 총액 × {@code penalty_rate%}, 원 단위 반올림(KRW 는 소수
 *       단위가 없다). 환불 = 총액 − 위약금.</li>
 * </ol>
 *
 * <p><b>왜 순수 계산기인가</b> — 위약금은 정책·날짜·금액의 함수일 뿐 부작용이 없다. 저장·재고·
 * 결제취소 같은 부작용은 호출자(취소 서비스)가 맡는다. 이렇게 갈라 두면 반올림·경계일 같은
 * 까다로운 부분을 실제 MySQL 없이 검증할 수 있다(사용자 검토 의도).
 */
public final class CancellationPolicy {

	private CancellationPolicy() {
	}

	/**
	 * 결제된(CONFIRMED) 예약의 취소 과금을 계산한다.
	 *
	 * @param plan        예약이 참조한 요금정책
	 * @param checkInDate 체크인(도착)일 — 무료 취소 기한의 기준
	 * @param totalAmount 결제 총액 — 위약금·환불의 모수
	 * @param asOf        취소 요청일(보통 오늘)
	 */
	public static CancellationCharge quote(
			RatePlan plan, LocalDate checkInDate, BigDecimal totalAmount, LocalDate asOf) {

		// 1) 환불 불가 정책 — 기한과 무관하게 전액 위약금.
		if (!plan.isRefundable()) {
			return new CancellationCharge(
					totalAmount, BigDecimal.ZERO, CancellationCharge.Basis.NON_REFUNDABLE);
		}

		// 2) 무료 취소 기한 이내 — 체크인 cancelDeadlineDays 일 전까지는 전액 환불.
		LocalDate freeUntil = checkInDate.minusDays(plan.getCancelDeadlineDays());
		if (!asOf.isAfter(freeUntil)) {
			return new CancellationCharge(
					BigDecimal.ZERO, totalAmount, CancellationCharge.Basis.FREE);
		}

		// 3) 기한 경과 — penalty_rate% 를 원 단위로 반올림. 총액을 넘지 않게 클램프한다
		//    (penalty_rate 는 DB CHECK 로 0~100 이지만, 방어적으로 상한을 못 박는다).
		BigDecimal penalty = totalAmount
				.multiply(plan.getPenaltyRate())
				.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
				.min(totalAmount)
				.max(BigDecimal.ZERO);
		return new CancellationCharge(
				penalty, totalAmount.subtract(penalty), CancellationCharge.Basis.DEADLINE_PASSED);
	}
}
