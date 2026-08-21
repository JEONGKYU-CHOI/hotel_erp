package io.github.jeongkyuchoi.hotel.erp.recommendation;

import io.github.jeongkyuchoi.hotel.erp.notification.EmailMessage;

/**
 * 추천 후보({@link MemberRecommendation})를 한 통의 메일({@link EmailMessage})로 만든다
 * (추천 메일 3단계의 경계면).
 *
 * <p><b>이 인터페이스가 3단계의 교체 지점이다.</b> 지금은 규칙 기반
 * {@link TemplateRecommendationMailComposer} 하나뿐이고, 나중에 Gemini 로 문구를 생성하는
 * 구현체를 추가해 이 빈만 갈아 끼우면 발송 파이프라인(4단계)은 그대로 둔 채 문구만 바뀐다 —
 * {@link io.github.jeongkyuchoi.hotel.erp.notification.EmailSender} 가 전송 수단을 교체하는 것과
 * 같은 규율이다.
 */
public interface RecommendationMailComposer {

	EmailMessage compose(MemberRecommendation recommendation);
}
