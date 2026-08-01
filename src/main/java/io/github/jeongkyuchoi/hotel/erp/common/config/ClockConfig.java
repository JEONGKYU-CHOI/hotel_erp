package io.github.jeongkyuchoi.hotel.erp.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시스템 시계 빈.
 *
 * <p>{@code LocalDate.now()}/{@code LocalTime.now()} 를 코드에 직접 박으면 "지금"에
 * 의존하는 로직(당일 예약 마감 등)을 결정론적으로 테스트할 수 없다. 시계를 빈으로
 * 주입받아 {@code now(clock)} 로 읽으면, 테스트에서 고정 시각의 {@link Clock} 으로
 * 바꿔 마감 경계(마감 직전 허용 / 마감 직후 차단)를 재현할 수 있다.
 */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
