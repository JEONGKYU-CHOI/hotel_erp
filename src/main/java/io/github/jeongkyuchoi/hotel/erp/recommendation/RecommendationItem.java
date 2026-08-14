package io.github.jeongkyuchoi.hotel.erp.recommendation;

/**
 * 추천 후보 하나 — 객실타입 + 요금제 한 쌍(추천 메일 2단계). 3단계 Gemini 문구 생성의 입력이다.
 *
 * @param roomTypeId   객실타입 id
 * @param roomTypeName 객실타입명(문구·링크 표시용)
 * @param ratePlanId   요금제 id
 * @param ratePlanName 요금제명
 * @param source       후보 출처(본인 이력/세그먼트 인기). 문구 어투를 가른다.
 */
public record RecommendationItem(
		long roomTypeId, String roomTypeName,
		long ratePlanId, String ratePlanName,
		RecommendationSource source) {

	/** 중복 제거 키 — 같은 객실타입·요금제면 출처가 달라도 한 후보다. */
	record Key(long roomTypeId, long ratePlanId) {
	}

	Key key() {
		return new Key(roomTypeId, ratePlanId);
	}
}
