package io.github.jeongkyuchoi.hotel.erp.recommendation;

import io.github.jeongkyuchoi.hotel.erp.notification.EmailMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 규칙 기반 추천 메일 문구(3단계 임시 구현). Gemini 문구 생성이 준비되기 전까지 파이프라인을
 * end-to-end 로 돌리기 위한 대체물이다 — 나중에 Gemini 구현체를 {@code @Primary} 로 더하면
 * 이 빈은 물러난다({@link RecommendationMailComposer} 주석 참조).
 *
 * <p>후보 출처로 어투를 가른다 — {@link RecommendationSource#HISTORY} 는 재방문 유도,
 * {@link RecommendationSource#POPULAR} 는 첫 제안. 하단에는 회원 토큰이 박힌 수신거부 링크를
 * 붙인다({@code /api/marketing/unsubscribe?token=...}).
 */
@Component
public class TemplateRecommendationMailComposer implements RecommendationMailComposer {

	private static final String BRAND = "더 스테이";

	private final String publicBaseUrl;

	public TemplateRecommendationMailComposer(
			@Value("${app.public-base-url}") String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}

	@Override
	public EmailMessage compose(MemberRecommendation rec) {
		String subject = "[" + BRAND + "] " + rec.name() + "님을 위한 이번 달 추천 객실";

		StringBuilder body = new StringBuilder()
				.append(rec.name()).append("님, 안녕하세요. ").append(BRAND).append("입니다.\n\n")
				.append("고객님께 어울릴 객실을 골라 보았어요.\n\n");
		for (RecommendationItem item : rec.items()) {
			body.append("• ").append(item.roomTypeName()).append(" · ").append(item.ratePlanName())
					.append("\n  ").append(pitch(item.source())).append("\n");
		}
		body.append("\n예약은 홈페이지에서 하실 수 있습니다. 즐거운 여정 되세요.\n\n")
				.append("─────────────\n")
				.append("추천 메일을 그만 받으시려면 아래 링크를 눌러 주세요.\n")
				.append(unsubscribeLink(rec.unsubscribeToken()));

		return new EmailMessage(rec.email(), subject, body.toString());
	}

	/** 출처별 한 줄 소개. HISTORY=재방문 유도, POPULAR=첫 제안. */
	private String pitch(RecommendationSource source) {
		return switch (source) {
			case HISTORY -> "지난번 편안하셨던 그 객실, 다시 모실 준비가 되어 있어요.";
			case POPULAR -> "요즘 고객님 또래에서 가장 사랑받는 객실이에요.";
		};
	}

	private String unsubscribeLink(String token) {
		String t = token == null ? "" : URLEncoder.encode(token, StandardCharsets.UTF_8);
		return publicBaseUrl + "/api/marketing/unsubscribe?token=" + t;
	}
}
