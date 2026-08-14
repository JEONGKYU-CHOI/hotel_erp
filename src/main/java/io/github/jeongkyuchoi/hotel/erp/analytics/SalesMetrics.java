package io.github.jeongkyuchoi.hotel.erp.analytics;

/**
 * 세그먼트 × 객실타입 × 요금제 한 조합의 월간 판매 지표(추천 메일 1단계 산출물).
 *
 * <p>상태를 세 갈래로 나눠 센다. {@code HOLD·EXPIRED·CANCELLED} 는 실제 판매가 아니므로
 * 어디에도 세지 않는다({@link SalesAggregationService} 의 필터에서 아예 빠진다).
 *
 * <ul>
 *   <li>{@code checkoutCount} — {@code CHECKED_OUT}. <b>추천 랭킹의 기준값</b>이다.
 *       실제 묵고 나간 예약만 "그 세그먼트가 정말 만족한 상품"의 근거가 된다.</li>
 *   <li>{@code reservedCount} — {@code CONFIRMED + CHECKED_IN}. 아직 안 나갔지만 확정된 수요.
 *       참고 지표.</li>
 *   <li>{@code noShowCount} — {@code NO_SHOW}. 예약해 놓고 오지 않은 수. 랭킹에서 감점·경고
 *       신호로 쓸 수 있는 참고 지표.</li>
 * </ul>
 *
 * @param segment      고객 세그먼트(성별×나이대, 또는 {@link Segment#GUEST})
 * @param roomTypeId   객실타입 id
 * @param roomTypeName 객실타입명
 * @param ratePlanId   요금제 id
 * @param ratePlanName 요금제명
 * @param checkoutCount 체크아웃 건수(추천 랭킹 기준)
 * @param reservedCount 예약(확정·투숙중) 건수
 * @param noShowCount   노쇼 건수
 */
public record SalesMetrics(
		Segment segment,
		long roomTypeId, String roomTypeName,
		long ratePlanId, String ratePlanName,
		long checkoutCount, long reservedCount, long noShowCount) {
}
