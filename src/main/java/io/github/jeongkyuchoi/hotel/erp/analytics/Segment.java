package io.github.jeongkyuchoi.hotel.erp.analytics;

import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Gender;

/**
 * 판매 지표를 나누는 고객 세그먼트 — 성별 × 나이대(추천 메일 근거 데이터).
 *
 * <p><b>비회원과 프로필 미상 회원은 {@link #GUEST} 한 칸으로 모은다</b> — 성별·생년월일이
 * 없으면 세그먼트를 특정할 수 없다. 비회원 예약이 기본 경로(D-009)이고 기존 회원은 프로필이
 * NULL 일 수 있으므로, 이들을 버리지 않고 "전체집계"로만 잡아 총량은 유지한다. 추천 후보
 * 선정(2단계)은 이 GUEST 세그먼트를 개인화 대상에서 제외하되 인기 랭킹 참고치로는 쓸 수 있다.
 *
 * @param gender  회원 성별. GUEST 세그먼트에서는 null.
 * @param ageBand 나이대 구간. GUEST 세그먼트에서는 null.
 */
public record Segment(Gender gender, AgeBand ageBand) {

	/** 비회원 또는 성별·생년월일이 없는 회원을 모으는 전체집계 세그먼트. */
	public static final Segment GUEST = new Segment(null, null);

	/** 성별·나이대가 모두 있는 세그먼트인가. false 면 {@link #GUEST}(전체집계)다. */
	public boolean isGuest() {
		return gender == null || ageBand == null;
	}
}
