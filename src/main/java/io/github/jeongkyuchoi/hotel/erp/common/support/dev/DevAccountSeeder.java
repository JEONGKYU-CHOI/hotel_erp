package io.github.jeongkyuchoi.hotel.erp.common.support.dev;

import io.github.jeongkyuchoi.hotel.erp.common.domain.member.StaffRole;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.StaffUser;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발용 백오피스 계정 시더.
 *
 * <p><b>실행</b>
 * <pre>./gradlew bootRun --args='--spring.profiles.active=dev'</pre>
 *
 * <p><b>왜 Flyway 마이그레이션에 넣지 않았는가</b><br>
 * 시드를 {@code V2__seed.sql} 로 만들면 비밀번호 해시가 리포지토리에 커밋된다.
 * 데모용이라도 자격증명을 형상관리에 남기지 않는 편이 낫고, 무엇보다 마이그레이션은
 * <b>모든 환경에서 실행된다</b> — 나중에 어딘가에 배포하면 알려진 비밀번호를 가진
 * 관리자 계정이 함께 따라간다. {@code @Profile("dev")} 로 두면 프로파일을 명시하지
 * 않는 한 이 코드는 로딩조차 되지 않는다.
 *
 * <p>계정이 이미 있으면 아무 것도 하지 않는다. 비밀번호를 바꿔 두었는데 재기동할 때마다
 * 되돌아가면 곤란하기 때문이다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevAccountSeeder implements ApplicationRunner {

	private static final Long TENANT_ID = 1L;
	private static final String ADMIN_USERNAME = "admin";
	private static final String STAFF_USERNAME = "staff";
	/** 개발 전용. 외부에 노출되는 환경에서는 절대 쓰지 않는다. */
	private static final String DEV_PASSWORD = "dev1234!";

	private final StaffUserRepository staffUserRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seed(ADMIN_USERNAME, "개발관리자", StaffRole.ADMIN);
		seed(STAFF_USERNAME, "개발프론트", StaffRole.STAFF);
	}

	private void seed(String username, String name, StaffRole role) {
		if (staffUserRepository.existsByTenantIdAndUsername(TENANT_ID, username)) {
			log.info("[dev] 계정 '{}' 이미 존재 — 건너뜀", username);
			return;
		}
		staffUserRepository.save(StaffUser.builder()
				.tenantId(TENANT_ID)
				.username(username)
				// 평문이 DB 에 닿는 경로는 여기 한 곳뿐이고, 즉시 해시로 바뀐다.
				.passwordHash(passwordEncoder.encode(DEV_PASSWORD))
				.name(name)
				.role(role)
				.active(true)
				.build());
		log.warn("[dev] 개발용 계정 생성: {} / {} (role={}) — 개발 환경 전용이다",
				username, DEV_PASSWORD, role);
	}
}
