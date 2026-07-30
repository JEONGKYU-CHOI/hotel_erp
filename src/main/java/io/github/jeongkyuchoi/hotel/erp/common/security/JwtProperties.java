package io.github.jeongkyuchoi.hotel.erp.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 (D-008). {@code application.yml} 의 {@code app.jwt.*} 를 바인딩한다.
 *
 * @param secret              HS256 서명 키. 운영은 환경변수로 주입한다. 256bit 이상이어야 한다.
 * @param accessTokenValidity Access Token 수명. 리프레시 토큰은 두지 않는다(D-010).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenValidity) {
}
