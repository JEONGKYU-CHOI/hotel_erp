package io.github.jeongkyuchoi.hotel.erp.common.support.demo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Member;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 데모 기준 데이터 시더 (배포/시연용).
 *
 * <p><b>왜 있는가</b><br>
 * 배포(예: docker compose)에서 MySQL 컨테이너는 매번 빈 스키마로 시작한다. 부킹엔진·관리자
 * 대시보드를 시연하려면 객실타입·요금제·호실·재고가 있어야 한다. Flyway 가 스키마를 만든 뒤,
 * 이 러너가 데모 데이터를 채운다.
 *
 * <p><b>왜 마이그레이션이 아니라 프로파일 시더인가</b><br>
 * 데모 데이터는 스키마의 일부가 아니라 "예시"다. 마이그레이션에 넣으면 모든 환경에 따라간다.
 * {@code @Profile("demo")} 로 두면 그 프로파일을 켤 때만 로딩된다.
 *
 * <p><b>멱등</b> — 데모 객실타입(STDT)이 이미 있으면 아무 것도 하지 않는다. 재기동·볼륨 유지
 * 상태에서 중복 삽입(코드 UNIQUE 위반)을 막는다. 시드 SQL 은 날짜를 CURDATE() 상대값으로
 * 만들므로 언제 띄워도 "오늘~+60일" 재고가 유효하다.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

	private static final String SEED_SCRIPT = "db/demo/demo-seed.sql";

	/** 시연용 회원 계정. 로그인 화면에서 바로 써 볼 수 있게 고정한다. */
	private static final Long TENANT_ID = 1L;
	private static final String DEMO_EMAIL = "demo@thestay.example";
	private static final String DEMO_PASSWORD = "demo1234!";

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(ApplicationArguments args) {
		seedBaseData();
		seedDemoMember();
	}

	private void seedBaseData() {
		Integer existing = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM room_type WHERE tenant_id = 1 AND code = 'STDT'", Integer.class);
		if (existing != null && existing > 0) {
			log.info("[demo] 데모 데이터 이미 존재 — 건너뜀");
			return;
		}
		try (Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, new ClassPathResource(SEED_SCRIPT));
			log.info("[demo] 데모 데이터 시드 완료 (객실타입 3종·요금제·호실·재고 오늘~+60일)");
		} catch (SQLException e) {
			throw new IllegalStateException("데모 데이터 시드 실패: " + SEED_SCRIPT, e);
		}
	}

	/**
	 * 시연용 회원 계정을 심는다 — 비번은 SQL 로 만들 수 없어(bcrypt) 여기서 인코딩한다.
	 * 이메일로 멱등 확인해 재기동 시 중복 생성을 막는다.
	 */
	private void seedDemoMember() {
		if (memberRepository.existsByTenantIdAndEmail(TENANT_ID, DEMO_EMAIL)) {
			log.info("[demo] 데모 회원 이미 존재 — 건너뜀");
			return;
		}
		memberRepository.save(Member.builder()
				.tenantId(TENANT_ID)
				.email(DEMO_EMAIL)
				.passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
				.name("데모회원")
				.phone("010-1234-5678")
				.status(MemberStatus.ACTIVE)
				.build());
		log.info("[demo] 데모 회원 시드 완료 — {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
	}
}
