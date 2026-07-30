package io.github.jeongkyuchoi.hotel.erp.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 토스페이먼츠 설정 (D-034). {@code application.yml} 의 {@code app.toss.*} 를 바인딩한다.
 *
 * @param secretKey 시크릿 키. 서버가 승인 API 호출·웹훅 검증에 쓴다. 운영·개발 모두 환경변수
 *                  {@code TOSS_SECRET_KEY} 로 주입한다 — 절대 파일/커밋에 실제 값을 두지 않는다.
 * @param clientKey 결제창(프론트)용 공개 키. 백엔드는 쓰지 않으나 React 단계에서 내려주려고 둔다.
 * @param baseUrl   토스 결제 API 베이스. 승인 엔드포인트는 {@code /v1/payments/confirm}.
 */
@ConfigurationProperties(prefix = "app.toss")
public record TossProperties(String secretKey, String clientKey, String baseUrl) {
}
