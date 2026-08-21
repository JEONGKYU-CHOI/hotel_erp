package io.github.jeongkyuchoi.hotel.erp.recommendation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 추천 메일 설정. {@link GeminiProperties} 바인딩을 켠다(토스의 PaymentConfig 와 같은 방식). */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class RecommendationConfig {
}
