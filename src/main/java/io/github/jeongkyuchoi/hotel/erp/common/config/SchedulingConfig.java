package io.github.jeongkyuchoi.hotel.erp.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @Scheduled} 활성화.
 *
 * <p>메인 클래스가 아니라 별도 설정으로 분리한 이유 — 스케줄링을 켜고 끄는 지점을 한 곳에
 * 명시적으로 둔다. 통합테스트에서 스케줄러의 자동 실행이 방해되면 이 설정만 제외하면 된다.
 *
 * <p>현재 스케줄 작업: HOLD 만료 청소({@code HoldExpiryScheduler}, D-003).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
