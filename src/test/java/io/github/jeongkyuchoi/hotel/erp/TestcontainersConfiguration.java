package io.github.jeongkyuchoi.hotel.erp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합테스트용 MySQL 컨테이너 (D-006).
 *
 * <p>H2 를 쓰지 않는 이유: H2 는 InnoDB 의 갭 락/넥스트키 락을 재현하지 못하고
 * {@code SELECT ... FOR UPDATE} 의 동작도 다르다. "오버부킹 0건"이 이 프로젝트의
 * 핵심 주장인데 그 증명을 다른 DB 에서 하면 주장 자체가 성립하지 않는다.
 *
 * <p>이미지 태그를 {@code latest} 가 아니라 운영과 동일한 버전으로 고정한다.
 * {@code latest} 는 시점에 따라 MySQL 9.x 를 받아올 수 있고, 그러면 테스트가
 * 통과해도 운영(8.4)과 다른 엔진에서 검증한 것이 되어 의미가 없다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	/** 로컬 개발 서버와 동일한 버전으로 고정한다. */
	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

	// Testcontainers 2.x 의 MySQLContainer 는 제네릭이 아니다(1.x 의 <SELF> 패턴 제거됨).
	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(MYSQL_IMAGE)
				.withDatabaseName("hotel_erp")
				// 서버 설정을 운영과 맞춘다. 특히 시간대는 영업일자 계산에 직결된다.
				.withCommand(
						"--character-set-server=utf8mb4",
						"--collation-server=utf8mb4_0900_ai_ci",
						"--default-time-zone=+09:00");
	}

}
