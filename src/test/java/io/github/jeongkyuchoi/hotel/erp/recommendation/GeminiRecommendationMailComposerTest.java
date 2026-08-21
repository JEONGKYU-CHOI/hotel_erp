package io.github.jeongkyuchoi.hotel.erp.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jeongkyuchoi.hotel.erp.recommendation.GeminiRecommendationMailComposer.Composed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Gemini 응답 파싱 단위 테스트(추천 메일 3단계). HTTP 없이 응답 봉투 추출·문구 JSON 파싱만
 * 검증한다 — 실호출·폴백 wiring 은 {@link RecommendationMailDispatcherTest}(키 없음 → 템플릿)가 덮는다.
 */
class GeminiRecommendationMailComposerTest {

	@Test
	@DisplayName("응답 봉투에서 candidates[0].content.parts[0].text 를 꺼낸다")
	void extractsText() {
		String envelope = """
				{"candidates":[{"content":{"parts":[{"text":"{\\"subject\\":\\"제목\\",\\"body\\":\\"본문\\"}"}]}}]}
				""";
		String text = GeminiRecommendationMailComposer.extractText(envelope);
		assertThat(text).contains("subject").contains("본문");
	}

	@Test
	@DisplayName("텍스트가 없는 응답은 예외")
	void extractText_missing() {
		assertThatThrownBy(() -> GeminiRecommendationMailComposer.extractText("{\"candidates\":[]}"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("순수 JSON 을 subject/body 로 파싱")
	void parsesPlainJson() {
		Composed c = GeminiRecommendationMailComposer.parse(
				"{\"subject\":\"이번 달 추천\",\"body\":\"안녕하세요\"}");
		assertThat(c.subject()).isEqualTo("이번 달 추천");
		assertThat(c.body()).isEqualTo("안녕하세요");
	}

	@Test
	@DisplayName("코드펜스로 감싼 JSON 도 벗겨서 파싱")
	void parsesFencedJson() {
		String fenced = "```json\n{\"subject\":\"제목\",\"body\":\"본문 내용\"}\n```";
		Composed c = GeminiRecommendationMailComposer.parse(fenced);
		assertThat(c.subject()).isEqualTo("제목");
		assertThat(c.body()).isEqualTo("본문 내용");
	}

	@Test
	@DisplayName("subject/body 누락은 예외")
	void parse_missingFields() {
		assertThatThrownBy(() -> GeminiRecommendationMailComposer.parse("{\"subject\":\"제목만\"}"))
				.isInstanceOf(IllegalStateException.class);
	}
}
