package com.hotel.erp.common.support.spike;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Day 0 비관적 락 스파이크 진입점.
 *
 * <p><b>목적</b> — JPA 실무 경험이 없는 상태에서 Day 2(동시성)에 락을 처음 만나면
 * 하루가 날아간다. 재고 락이 이 프로젝트의 핵심 주장이므로, 도메인 코드를 쓰기 전에
 * 다음 네 가지를 <em>로그로 실측</em>해 둔다.
 *
 * <ol>
 *   <li>{@code @Lock(PESSIMISTIC_WRITE)} 가 실제로 {@code for update} 를 붙여 보내는가</li>
 *   <li>native query 의 {@code FOR UPDATE} 와 결과가 같은가</li>
 *   <li>락이 정말로 다른 커넥션을 대기시키는가 (SQL 에 찍히는 것과 별개)</li>
 *   <li>영속성 컨텍스트 1차 캐시 때문에 옛 값을 읽게 되는가</li>
 * </ol>
 *
 * <p><b>실측 결과 (2026-07-29)</b>
 * <ol>
 *   <li>붙는다. JPQL 에는 {@code for update of ri1_0}, native 에는 {@code FOR UPDATE}.
 *       바인딩 파라미터도 함께 찍힌다.</li>
 *   <li>같다. 둘 다 같은 행을 {@code stay_date} 오름차순으로 잠근다.</li>
 *   <li>막는다. 락을 잡은 채 3초 대기하는 동안 외부 커넥션의 UPDATE 가 진행되지 못했고,
 *       트랜잭션 커밋 직후 3012ms 만에 완료됐다.</li>
 *   <li><b>재현됐다. 그리고 예상보다 나빴다.</b> 원인이 두 겹이다 —
 *       ① 이미 영속 상태인 인스턴스가 있으면 JPA 는 DB 가 돌려준 최신 행을 버리고
 *          캐시의 인스턴스를 반환한다.
 *       ② 락 없는 읽기는 MySQL REPEATABLE READ 스냅샷을 본다.
 *       {@code refresh(entity, PESSIMISTIC_WRITE)} 로도 회복되지 않았다
 *       (실측: for update 가 붙지 않은 평범한 select 가 나갔다).
 *       DB 실제 값이 2인 동안 트랜잭션 안에서는 끝까지 1 로 보였다.
 *       <b>→ 재고를 읽는 첫 조회가 락 조회여야 한다. 나중에 덧붙일 수 없다.</b></li>
 * </ol>
 *
 * <p><b>실행</b>
 * <pre>./gradlew bootRun --args='--spring.profiles.active=spike'</pre>
 * 시나리오가 끝나면 애플리케이션이 스스로 종료한다.
 *
 * <p>{@code @Profile("spike")} 로 격리해 평상시 기동에는 전혀 관여하지 않는다.
 * 스파이크 코드는 검증이 끝나면 지워도 되지만, "무엇을 어떻게 확인했는가"의 증거이므로
 * 남겨 둔다. 실행하려면 프로파일을 명시해야 하므로 사고로 돌아갈 일이 없다.
 */
@Slf4j
@Component
@Profile("spike")
@RequiredArgsConstructor
public class PessimisticLockSpike implements ApplicationRunner {

	private static final long TENANT_ID = 1L;
	/** 스파이크 전용 객실타입 코드. 한글을 쓰지 않는다 — 검증 데이터는 ASCII 로 둔다. */
	private static final String SPIKE_ROOM_TYPE_CODE = "SPIKE";

	private final JdbcTemplate jdbcTemplate;
	private final PessimisticLockSpikeScenarios scenarios;
	private final ConfigurableApplicationContext context;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		LocalDate from = LocalDate.now();
		LocalDate to = from.plusDays(2);

		Long roomTypeId = prepareFixture(from, to);
		log.info("=== 스파이크 시작: room_type_id={} 기간 {} ~ {} ===", roomTypeId, from, to);

		scenarios.scenario1_jpqlLock(roomTypeId, from, to);
		scenarios.scenario2_nativeLock(roomTypeId, from, to);

		Thread outsider = scenarios.scenario3_lockActuallyBlocks(roomTypeId, from, to);
		// 트랜잭션이 커밋된 뒤 대기 중이던 UPDATE 가 풀린다. 그 로그까지 보고 넘어간다.
		outsider.join(10_000);

		scenarios.scenario4_firstLevelCacheTrap(roomTypeId, from);
		log.info("### [4-f] 트랜잭션 밖에서 읽은 DB 실제 값 sold_qty = {} "
				+ "(시나리오 3 에서 +1, 시나리오 4-b 에서 +1 → 2 여야 한다)",
				scenarios.readGroundTruth(roomTypeId, from));

		log.info("=== 스파이크 종료 ===");
		System.exit(SpringApplication.exit(context, () -> 0));
	}

	/**
	 * 스파이크용 기준 데이터를 만든다. 매 실행마다 같은 상태에서 출발하도록
	 * 재고 수량을 초기화한다 (시나리오가 sold_qty 를 증가시키기 때문).
	 */
	private Long prepareFixture(LocalDate from, LocalDate to) {
		Long roomTypeId = jdbcTemplate.query(
				"SELECT id FROM room_type WHERE tenant_id = ? AND code = ?",
				rs -> rs.next() ? rs.getLong(1) : null,
				TENANT_ID, SPIKE_ROOM_TYPE_CODE);

		if (roomTypeId == null) {
			jdbcTemplate.update("""
					INSERT INTO room_type
					  (tenant_id, code, name, standard_occupancy, max_occupancy,
					   display_order, active, created_at, updated_at, created_by, updated_by)
					VALUES (?, ?, 'Spike Room Type', 2, 2, 999, TRUE, NOW(), NOW(), 'SPIKE', 'SPIKE')
					""", TENANT_ID, SPIKE_ROOM_TYPE_CODE);
			roomTypeId = jdbcTemplate.queryForObject(
					"SELECT id FROM room_type WHERE tenant_id = ? AND code = ?",
					Long.class, TENANT_ID, SPIKE_ROOM_TYPE_CODE);
			log.info("스파이크용 room_type 생성: id={}", roomTypeId);
		}

		for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
			// 있으면 수량만 초기화, 없으면 생성. 유니크 키는 (room_type_id, stay_date, tenant_id).
			jdbcTemplate.update("""
					INSERT INTO room_inventory
					  (tenant_id, room_type_id, stay_date, total_qty, sold_qty, held_qty,
					   created_at, updated_at, created_by, updated_by)
					VALUES (?, ?, ?, 10, 0, 0, NOW(), NOW(), 'SPIKE', 'SPIKE')
					ON DUPLICATE KEY UPDATE
					  total_qty = 10, sold_qty = 0, held_qty = 0, updated_at = NOW()
					""", TENANT_ID, roomTypeId, d);
		}
		log.info("스파이크용 room_inventory 초기화 완료 (total=10, sold=0, held=0)");
		return roomTypeId;
	}
}
