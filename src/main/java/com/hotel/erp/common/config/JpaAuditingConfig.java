package com.hotel.erp.common.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JPA 감사(auditing) 설정.
 *
 * <p>{@code @EnableJpaAuditing} 이 있어야 {@code @CreatedDate}/{@code @CreatedBy} 등이
 * 실제로 채워진다. 이 애노테이션을 빠뜨리면 컬럼이 조용히 null 로 남고,
 * {@code nullable = false} 제약에 걸려 insert 가 실패한다. JPA 초심자가 자주 밟는 지점이다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

	/** 인증 주체가 없는 경로(배치, 초기 데이터 적재 등)에서 사용할 기본 작성자. */
	private static final String SYSTEM = "SYSTEM";

	/**
	 * 현재 로그인 사용자를 감사 컬럼에 채운다.
	 *
	 * <p>고객용 부킹엔진은 비회원 예약이 가능하고 야간마감은 배치가 돌리므로
	 * 인증 주체가 없는 경우가 정상적으로 존재한다. 그때는 {@code SYSTEM} 으로 기록한다.
	 */
	@Bean
	public AuditorAware<String> auditorProvider() {
		return () -> {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
				return Optional.of(SYSTEM);
			}
			return Optional.of(auth.getName());
		};
	}
}
