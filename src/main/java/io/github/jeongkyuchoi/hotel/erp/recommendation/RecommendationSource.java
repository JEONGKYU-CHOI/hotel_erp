package io.github.jeongkyuchoi.hotel.erp.recommendation;

/**
 * 추천 후보가 어디서 왔는지(추천 메일 2단계). 3단계 Gemini 문구 생성이 어투를 가른다 —
 * 재방문 유도인지 첫 제안인지.
 */
public enum RecommendationSource {

	/** 회원 본인이 과거에 체크아웃한 적 있는 객실타입·요금제(재방문 유도). */
	HISTORY,
	/** 회원이 속한 세그먼트에서 인기였던 객실타입·요금제(첫 제안). */
	POPULAR
}
