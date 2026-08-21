package io.github.jeongkyuchoi.hotel.erp.recommendation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemini 문구 생성 설정(추천 메일 3단계). {@code application.yml} 의 {@code app.gemini.*} 를
 * 바인딩한다.
 *
 * @param apiKey  Gemini API 키. <b>비어 있으면 실호출을 하지 않고 템플릿 문구로 폴백한다</b>
 *                ({@link GeminiRecommendationMailComposer}). 실제 값은 환경변수
 *                {@code GEMINI_API_KEY} 또는 {@code application-local.yml} 로만 주입한다 —
 *                절대 파일/커밋에 두지 않는다(토스 키와 같은 규율).
 * @param model   사용할 모델 이름(예: {@code gemini-2.0-flash}).
 * @param baseUrl Generative Language API 베이스 URL.
 */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(String apiKey, String model, String baseUrl) {

	/** 키가 실제로 주입됐는가. 공백/빈 문자열이면 폴백 대상이다. */
	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}
}
