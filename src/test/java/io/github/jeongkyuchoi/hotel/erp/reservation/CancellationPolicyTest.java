package io.github.jeongkyuchoi.hotel.erp.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.CancellationCharge;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.CancellationCharge.Basis;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.CancellationPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 취소 위약금 계산 규칙 테스트 (D-037).
 *
 * <p><b>순수 함수라 Spring·MySQL 없이 돈다.</b> 반올림·경계일 같은 까다로운 부분을 실제 DB
 * 없이 전 경계에서 못 박는 것이 계산기를 떼어 둔 이유다. 검증: ① 환불불가는 전액 위약금,
 * ② 무료취소 기한 이내(경계일 포함)는 전액 환불, ③ 기한 경과는 penalty_rate% 위약금,
 * ④ 원 단위 반올림(HALF_UP), ⑤ 위약금은 총액을 넘지 않는다.
 */
class CancellationPolicyTest {

	private static final LocalDate CHECK_IN = LocalDate.of(2026, 8, 10);

	/** penalty_rate·cancel_deadline_days·refundable 만 계산에 쓰이므로 나머지는 최소로 채운다. */
	private RatePlan plan(boolean refundable, int deadlineDays, String penaltyRate) {
		return RatePlan.builder()
				.tenantId(1L).roomType(null).code("PLAN").name("정책")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false)
				.refundable(refundable).cancelDeadlineDays((short) deadlineDays)
				.penaltyRate(new BigDecimal(penaltyRate)).active(true)
				.build();
	}

	@Test
	@DisplayName("환불불가 정책 → 위약금 100%, 환불 0 (기한 무관)")
	void nonRefundable_fullPenalty() {
		RatePlan plan = plan(false, 1, "0");
		BigDecimal total = new BigDecimal("120000.00");

		// 무료취소 기한 한참 전이어도 환불불가면 전액 위약금.
		CancellationCharge charge = CancellationPolicy.quote(
				plan, CHECK_IN, total, LocalDate.of(2026, 7, 1));

		assertThat(charge.basis()).isEqualTo(Basis.NON_REFUNDABLE);
		assertThat(charge.penalty()).isEqualByComparingTo(total);
		assertThat(charge.refund()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("무료취소 기한 이내 → 위약금 0, 전액 환불")
	void beforeDeadline_freeCancel() {
		RatePlan plan = plan(true, 3, "50"); // 체크인 3일 전(08-07)까지 무료
		BigDecimal total = new BigDecimal("120000.00");

		CancellationCharge charge = CancellationPolicy.quote(
				plan, CHECK_IN, total, LocalDate.of(2026, 8, 5));

		assertThat(charge.basis()).isEqualTo(Basis.FREE);
		assertThat(charge.penalty()).isEqualByComparingTo("0");
		assertThat(charge.refund()).isEqualByComparingTo(total);
	}

	@Test
	@DisplayName("무료취소 경계일(체크인-기한 당일)은 아직 무료")
	void onDeadlineBoundary_stillFree() {
		RatePlan plan = plan(true, 3, "50"); // freeUntil = 08-07
		CancellationCharge charge = CancellationPolicy.quote(
				plan, CHECK_IN, new BigDecimal("100000.00"), LocalDate.of(2026, 8, 7));

		assertThat(charge.basis()).isEqualTo(Basis.FREE);
		assertThat(charge.penalty()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("기한 경과 → penalty_rate% 위약금, 나머지 환불")
	void afterDeadline_percentPenalty() {
		RatePlan plan = plan(true, 3, "50"); // freeUntil = 08-07, 다음날부터 50%
		BigDecimal total = new BigDecimal("120000.00");

		CancellationCharge charge = CancellationPolicy.quote(
				plan, CHECK_IN, total, LocalDate.of(2026, 8, 8));

		assertThat(charge.basis()).isEqualTo(Basis.DEADLINE_PASSED);
		assertThat(charge.penalty()).isEqualByComparingTo("60000"); // 120000 × 50%
		assertThat(charge.refund()).isEqualByComparingTo("60000");
	}

	@Test
	@DisplayName("위약금은 원 단위로 반올림(HALF_UP)한다")
	void penalty_roundsToWon() {
		RatePlan plan = plan(true, 1, "10"); // freeUntil=08-09, 08-10부터 10% 위약금
		// 10005 × 10% = 1000.50 → HALF_UP → 1001
		CancellationCharge charge = CancellationPolicy.quote(
				plan, CHECK_IN, new BigDecimal("10005.00"), CHECK_IN);

		assertThat(charge.penalty()).isEqualByComparingTo("1001");
		assertThat(charge.refund()).isEqualByComparingTo("9004"); // 10005 - 1001
	}

	@Test
	@DisplayName("노쇼: 환불불가 정책 → 위약금 100%")
	void noShow_nonRefundable_fullPenalty() {
		RatePlan plan = plan(false, 1, "0");
		BigDecimal total = new BigDecimal("120000.00");

		CancellationCharge charge = CancellationPolicy.quoteNoShow(plan, total);

		assertThat(charge.basis()).isEqualTo(Basis.NON_REFUNDABLE);
		assertThat(charge.penalty()).isEqualByComparingTo(total);
		assertThat(charge.refund()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("노쇼: 환불가능 정책 → penalty_rate% 위약금")
	void noShow_refundable_percentPenalty() {
		RatePlan plan = plan(true, 1, "30");
		CancellationCharge charge = CancellationPolicy.quoteNoShow(plan, new BigDecimal("100000.00"));

		assertThat(charge.basis()).isEqualTo(Basis.DEADLINE_PASSED);
		assertThat(charge.penalty()).isEqualByComparingTo("30000");
		assertThat(charge.refund()).isEqualByComparingTo("70000");
	}

	@Test
	@DisplayName("노쇼는 무료취소 기한이 없다 — deadline 0 정책이어도 과금된다")
	void noShow_noFreeWindow_evenWhenDeadlineZero() {
		// 취소였다면 deadline 0 + 당일은 FREE 지만, 노쇼는 도착 후라 무료 구간이 없다.
		RatePlan plan = plan(true, 0, "40");
		CancellationCharge charge = CancellationPolicy.quoteNoShow(plan, new BigDecimal("100000.00"));

		assertThat(charge.basis()).isEqualTo(Basis.DEADLINE_PASSED);
		assertThat(charge.penalty()).isEqualByComparingTo("40000");
	}

	@Test
	@DisplayName("penalty_rate 100 → 위약금이 총액을 넘지 않는다")
	void fullRate_penaltyEqualsTotal() {
		RatePlan plan = plan(true, 1, "100");
		BigDecimal total = new BigDecimal("99000.00");

		CancellationCharge charge = CancellationPolicy.quote(
				plan, CHECK_IN, total, CHECK_IN); // 기한(08-09) 경과 아님? 확인 아래

		// freeUntil = 08-09, asOf = 08-10 → 경과. penalty = 100% = total.
		assertThat(charge.basis()).isEqualTo(Basis.DEADLINE_PASSED);
		assertThat(charge.penalty()).isEqualByComparingTo(total);
		assertThat(charge.refund()).isEqualByComparingTo("0");
	}
}
