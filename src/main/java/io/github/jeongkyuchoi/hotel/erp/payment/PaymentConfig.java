package io.github.jeongkyuchoi.hotel.erp.payment;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 결제 모듈 설정 (D-034). {@link TossProperties} 바인딩을 켠다.
 *
 * <p>{@code RestClient.Builder} 는 Spring Boot 가 spring-web 존재 시 자동 구성하므로
 * 여기서 따로 빈을 두지 않고 {@code TossPaymentClient} 가 주입받아 쓴다.
 */
@Configuration
@EnableConfigurationProperties(TossProperties.class)
public class PaymentConfig {
}
