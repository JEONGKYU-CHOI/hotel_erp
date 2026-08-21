package io.github.jeongkyuchoi.hotel.erp.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jeongkyuchoi.hotel.erp.analytics.AgeBand;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Gender;
import io.github.jeongkyuchoi.hotel.erp.notification.EmailMessage;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Gemini 로 추천 메일 문구를 생성한다(추천 메일 3단계). {@link RecommendationMailComposer} 의
 * 실제(비-폴백) 구현이며 {@code @Primary} 라, 발송·미리보기가 주입받는 문구 생성기는 이것이다.
 *
 * <p><b>키가 없으면 통째로 폴백한다.</b> {@code app.gemini.api-key} 가 비어 있으면 실호출을
 * 하지 않고 {@link TemplateRecommendationMailComposer}(규칙 기반)로 넘긴다. 키가 있어도
 * 호출·파싱이 실패하면 같은 폴백으로 떨어진다 — 문구 생성이 실패해도 발송 파이프라인(4단계)은
 * 멈추지 않는다({@link io.github.jeongkyuchoi.hotel.erp.notification.EmailSender} 가 전송 수단을
 * 갈아 끼우는 것과 같은 규율).
 *
 * <p>수신거부 링크는 모델이 아니라 코드가 붙인다({@link TemplateRecommendationMailComposer#unsubscribeFooter}).
 * 링크·토큰은 정확성이 중요하므로 생성 문구에 맡기지 않는다.
 */
@Slf4j
@Component
@Primary
public class GeminiRecommendationMailComposer implements RecommendationMailComposer {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final GeminiProperties properties;
	private final TemplateRecommendationMailComposer fallback;
	private final RestClient restClient;

	public GeminiRecommendationMailComposer(GeminiProperties properties,
			TemplateRecommendationMailComposer fallback) {
		this.properties = properties;
		this.fallback = fallback;
		// baseUrl·JSON 직렬화만 쓰므로 정적 빌더로 충분하다(토스 클라이언트와 같은 방식).
		// 키가 없어도 빈 생성은 되게 두고, 실호출 직전에 hasApiKey 로 막는다.
		this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
	}

	@Override
	public EmailMessage compose(MemberRecommendation rec) {
		if (!properties.hasApiKey()) {
			return fallback.compose(rec); // 키 미설정 — 템플릿 문구로.
		}
		try {
			String modelText = callGemini(buildPrompt(rec));
			Composed composed = parse(modelText);
			String body = composed.body() + fallback.unsubscribeFooter(rec.unsubscribeToken());
			return new EmailMessage(rec.email(), composed.subject(), body);
		} catch (RuntimeException e) {
			log.warn("Gemini 문구 생성 실패 memberId={} — 템플릿으로 폴백", rec.memberId(), e);
			return fallback.compose(rec);
		}
	}

	/** Gemini {@code generateContent} 호출 → 모델이 낸 텍스트(JSON 문자열)를 돌려준다. */
	private String callGemini(String prompt) {
		Map<String, Object> request = Map.of(
				"contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
				"generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.7));

		String response = restClient.post()
				.uri("/v1beta/models/{model}:generateContent", properties.model())
				.header("x-goog-api-key", properties.apiKey())
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(String.class);

		return extractText(response);
	}

	/** Gemini 응답 봉투에서 첫 후보의 텍스트를 꺼낸다: {@code candidates[0].content.parts[0].text}. */
	static String extractText(String response) {
		try {
			JsonNode text = OBJECT_MAPPER.readTree(response)
					.path("candidates").path(0).path("content").path("parts").path(0).path("text");
			if (text.isMissingNode() || text.asText().isBlank()) {
				throw new IllegalStateException("Gemini 응답에 텍스트가 없습니다: " + response);
			}
			return text.asText();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Gemini 응답 파싱 실패", e);
		}
	}

	/** 모델이 낸 JSON 텍스트를 {@code {subject, body}} 로 파싱한다. 코드펜스가 섞여도 벗겨 낸다. */
	static Composed parse(String modelText) {
		String json = stripFences(modelText);
		try {
			JsonNode node = OBJECT_MAPPER.readTree(json);
			String subject = node.path("subject").asText(null);
			String body = node.path("body").asText(null);
			if (subject == null || subject.isBlank() || body == null || body.isBlank()) {
				throw new IllegalStateException("subject/body 누락: " + modelText);
			}
			return new Composed(subject.strip(), body.strip());
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("문구 JSON 파싱 실패: " + modelText, e);
		}
	}

	/** ```json … ``` 코드펜스를 벗겨 순수 JSON 만 남긴다(responseMimeType 을 무시하는 응답 대비). */
	private static String stripFences(String text) {
		String t = text.strip();
		if (t.startsWith("```")) {
			int firstNl = t.indexOf('\n');
			if (firstNl > 0) {
				t = t.substring(firstNl + 1);
			}
			if (t.endsWith("```")) {
				t = t.substring(0, t.length() - 3);
			}
		}
		return t.strip();
	}

	private String buildPrompt(MemberRecommendation rec) {
		StringBuilder items = new StringBuilder();
		for (RecommendationItem item : rec.items()) {
			String origin = switch (item.source()) {
				case HISTORY -> "예전에 묵으신 객실(재방문 유도)";
				case POPULAR -> "같은 또래에게 인기(첫 제안)";
			};
			items.append("- ").append(item.roomTypeName()).append(" · ").append(item.ratePlanName())
					.append(" [출처: ").append(origin).append("]\n");
		}
		return """
				당신은 호텔 '더 스테이'의 마케팅 카피라이터입니다.
				아래 회원에게 보낼 추천 메일의 제목과 본문을 한국어로 작성하세요.

				회원 이름: %s
				회원 세그먼트: %s
				추천 객실 목록:
				%s
				작성 규칙:
				- 따뜻하고 간결하게. 과장·이모지 남발 금지.
				- 출처가 '재방문 유도'인 객실은 다시 모시게 되어 반가운 어투로,
				  '첫 제안'인 객실은 새로 권하는 어투로 소개하세요.
				- 인사말은 "%s님"으로 시작하세요.
				- 제목은 40자 이내.
				- 수신거부 안내나 링크는 넣지 마세요. 시스템이 본문 끝에 자동으로 추가합니다.
				- 반드시 다음 JSON 형식만 출력하세요: {"subject": "...", "body": "..."}
				""".formatted(rec.name(), segmentText(rec), items, rec.name());
	}

	/** 세그먼트를 사람이 읽는 문구로: "남성 30대" 처럼. */
	private String segmentText(MemberRecommendation rec) {
		Gender g = rec.segment().gender();
		AgeBand b = rec.segment().ageBand();
		String genderText = g == Gender.MALE ? "남성" : g == Gender.FEMALE ? "여성" : "";
		String ageText = b == null ? "" : b == AgeBand.FIFTIES_PLUS ? "50대 이상" : b.floor() + "대";
		return (genderText + " " + ageText).strip();
	}

	/** 모델이 낸 제목·본문. */
	record Composed(String subject, String body) {
	}
}
