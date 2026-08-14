package io.github.jeongkyuchoi.hotel.erp.recommendation;

import io.github.jeongkyuchoi.hotel.erp.analytics.Segment;
import java.util.List;

/**
 * 회원 한 명의 추천 후보 묶음(추천 메일 2단계 산출물). 3단계 Gemini 문구 생성 → 4단계 발송으로
 * 넘어간다.
 *
 * <p>회원 엔티티 대신 발송에 필요한 필드만 담는다 — OSIV 를 껐으므로({@code open-in-view: false})
 * 상위 계층에서 지연로딩 연관을 건드리면 예외가 나고, 추천 파이프라인이 회원 엔티티의 생명주기에
 * 묶일 이유도 없다. {@code unsubscribeToken} 은 4단계 메일의 수신거부 링크에 쓴다.
 *
 * @param memberId         회원 id
 * @param email            수신 이메일
 * @param name             인사말용 이름
 * @param unsubscribeToken 수신거부 링크 토큰
 * @param segment          이 회원이 매핑된 세그먼트(성별×나이대)
 * @param items            추천 후보(본인 이력 우선, 인기로 채움). 최대 3개, 최소 1개.
 */
public record MemberRecommendation(
		Long memberId, String email, String name, String unsubscribeToken,
		Segment segment, List<RecommendationItem> items) {
}
