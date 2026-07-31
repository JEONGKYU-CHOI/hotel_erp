package io.github.jeongkyuchoi.hotel.erp.common.support.prod;

import io.github.jeongkyuchoi.hotel.erp.common.domain.member.StaffRole;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.StaffUser;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영/배포용 백오피스 관리자 시더.
 *
 * <p><b>왜 DevAccountSeeder 를 못 쓰는가</b><br>
 * {@link io.github.jeongkyuchoi.hotel.erp.common.support.dev.DevAccountSeeder} 는 비밀번호가
 * 소스에 박혀 있다({@code dev1234!}). 터널·클라우드처럼 외부에 노출되는 환경에서 그 계정을
 * 켜면 "누구나 아는 비밀번호의 관리자"가 열린다. 그래서 배포에서는 dev 프로파일을 켜지 않는다.
 *
 * <p>이 시더는 {@code @Profile("prod")} 에서만 돌고, 관리자 비밀번호를 <b>환경변수
 * {@code ADMIN_INIT_PASSWORD}</b> 로 주입받는다. 소스·커밋에 자격증명이 남지 않는다.
 *
 * <p><b>안전장치</b>
 * <ul>
 *   <li>비밀번호 미주입 시 계정을 만들지 않고 경고만 남긴다 — 약한 기본 비번을 만들지 않는다.</li>
 *   <li>이미 있으면 건너뛴다(멱등). 운영 중 바꾼 비번을 재기동이 되돌리지 않는다.</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdAdminSeeder implements ApplicationRunner {

	private static final Long TENANT_ID = 1L;

	private final StaffUserRepository staffUserRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.admin.username:admin}")
	private String username;

	@Value("${app.admin.password:}")
	private String password;

	@Value("${app.admin.name:관리자}")
	private String name;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (password == null || password.isBlank()) {
			log.warn("[prod] ADMIN_INIT_PASSWORD 미설정 — 관리자 계정을 만들지 않는다. "
					+ "백오피스(/admin) 로그인이 불가하니, 환경변수를 채워 재기동할 것.");
			return;
		}
		if (staffUserRepository.existsByTenantIdAndUsername(TENANT_ID, username)) {
			log.info("[prod] 관리자 '{}' 이미 존재 — 건너뜀", username);
			return;
		}
		staffUserRepository.save(StaffUser.builder()
				.tenantId(TENANT_ID)
				.username(username)
				.passwordHash(passwordEncoder.encode(password))
				.name(name)
				.role(StaffRole.ADMIN)
				.active(true)
				.build());
		log.info("[prod] 관리자 계정 생성: {} (비밀번호는 환경변수로 주입됨)", username);
	}
}
