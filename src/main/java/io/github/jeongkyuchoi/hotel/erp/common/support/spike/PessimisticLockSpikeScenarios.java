package io.github.jeongkyuchoi.hotel.erp.common.support.spike;

import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비관적 락 스파이크의 시나리오 본체.
 *
 * <p>{@link PessimisticLockSpike} 와 클래스를 분리한 이유: {@code @Transactional} 은
 * 스프링이 만든 프록시를 거칠 때만 동작한다. 같은 클래스 안에서 메서드를 직접 호출하면
 * (self-invocation) 프록시를 거치지 않아 트랜잭션이 아예 시작되지 않는다.
 * 그러면 락을 확인하려던 스파이크가 "락이 안 걸린다"는 잘못된 결론을 낸다.
 * JPA/스프링 입문자가 가장 자주 밟는 함정이라 구조로 막아 둔다.
 *
 * <p>시나리오마다 트랜잭션을 새로 여는 것도 의도적이다. 영속성 컨텍스트(1차 캐시)는
 * 트랜잭션 범위이므로, 한 트랜잭션에서 전부 돌리면 앞 시나리오가 캐시를 오염시킨다.
 */
@Slf4j
@Component
@Profile("spike")
@RequiredArgsConstructor
public class PessimisticLockSpikeScenarios {

	private final RoomInventoryRepository repository;
	private final EntityManager entityManager;
	private final DataSource dataSource;

	// -------------------------------------------------------------------------
	// 시나리오 1 : @Lock(PESSIMISTIC_WRITE) 가 실제로 for update 를 내보내는가
	// -------------------------------------------------------------------------
	@Transactional
	public void scenario1_jpqlLock(Long roomTypeId, LocalDate from, LocalDate to) {
		log.info("### [1-a] 락 없는 일반 조회 (대조군) ###");
		List<RoomInventory> plain =
				repository.findByRoomTypeIdAndStayDateBetweenOrderByStayDate(roomTypeId, from, to);
		log.info("### [1-a] 결과 {}건 — 위 SQL 에 for update 가 없어야 정상", plain.size());

		// 대조군이 남긴 엔티티가 다음 조회 결과를 덮어쓰지 않도록 컨텍스트를 비운다.
		// (이 clear 를 빼면 시나리오 3 의 함정이 여기서 미리 발생한다.)
		entityManager.clear();

		log.info("### [1-b] @Lock(PESSIMISTIC_WRITE) + JPQL ###");
		List<RoomInventory> locked = repository.lockForUpdate(roomTypeId, from, to);
		log.info("### [1-b] 결과 {}건 — 위 SQL 끝에 for update 가 붙어야 한다", locked.size());
	}

	// -------------------------------------------------------------------------
	// 시나리오 2 : native query 로 직접 쓴 FOR UPDATE (D-002 가 택한 방식)
	// -------------------------------------------------------------------------
	@Transactional
	public void scenario2_nativeLock(Long roomTypeId, LocalDate from, LocalDate to) {
		log.info("### [2] native query FOR UPDATE ###");
		List<RoomInventory> locked = repository.lockForUpdateNative(roomTypeId, from, to);
		locked.forEach(i -> log.info("### [2] {} total={} sold={} held={} available={}",
				i.getStayDate(), i.getTotalQty(), i.getSoldQty(), i.getHeldQty(), i.availableQty()));
	}

	// -------------------------------------------------------------------------
	// 시나리오 3 : 락이 정말로 다른 트랜잭션을 막는가
	//
	//   SQL 에 for update 가 찍혔다는 것만으로는 부족하다. 다른 커넥션이 실제로
	//   대기하는지 봐야 "락을 잡았다"고 말할 수 있다.
	//   별도 스레드에서 같은 행을 UPDATE 하고, 이 트랜잭션이 커밋되기 전까지
	//   그 UPDATE 가 끝나지 않는 것을 확인한다.
	// -------------------------------------------------------------------------
	@Transactional
	public Thread scenario3_lockActuallyBlocks(Long roomTypeId, LocalDate from, LocalDate to) {
		log.info("### [3] 락 획득 후 외부 커넥션 UPDATE 가 대기하는지 확인 ###");
		repository.lockForUpdateNative(roomTypeId, from, to);
		log.info("### [3] 락 획득 완료. 외부 스레드 기동");

		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(1);

		Thread outsider = new Thread(() -> {
			long begin = System.currentTimeMillis();
			// 트랜잭션 밖의 별도 커넥션이다. 스프링 트랜잭션에 묶인 커넥션을 쓰면
			// 같은 트랜잭션이 되어 락 경합 자체가 발생하지 않는다.
			try (Connection conn = dataSource.getConnection();
					PreparedStatement ps = conn.prepareStatement(
							"UPDATE room_inventory SET sold_qty = sold_qty + 1 "
									+ "WHERE room_type_id = ? AND stay_date = ?")) {
				conn.setAutoCommit(true);
				ps.setLong(1, roomTypeId);
				ps.setObject(2, from);
				started.countDown();
				ps.executeUpdate();
				log.info("### [3] 외부 UPDATE 완료 — 대기 {}ms", System.currentTimeMillis() - begin);
			} catch (Exception e) {
				log.error("### [3] 외부 UPDATE 실패: {}", e.getMessage());
			} finally {
				finished.countDown();
			}
		}, "spike-outsider");
		outsider.start();

		try {
			started.await(3, TimeUnit.SECONDS);
			boolean done = finished.await(3, TimeUnit.SECONDS);
			if (done) {
				log.error("### [3] 판정: 실패 — 외부 UPDATE 가 통과했다. 락이 걸리지 않았다.");
			} else {
				log.info("### [3] 판정: 성공 — 3초 동안 외부 UPDATE 가 대기 중. 락이 실제로 걸렸다.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// 이 메서드가 반환되면 트랜잭션이 커밋되고 대기 중이던 UPDATE 가 풀린다.
		return outsider;
	}

	// -------------------------------------------------------------------------
	// 시나리오 4 : 영속성 컨텍스트 1차 캐시 함정  ★ 가장 중요한 확인
	//
	//   락을 잡았다고 믿고 오래된 값을 읽는 사고가 여기서 난다.
	//   같은 트랜잭션에서 이미 조회한 엔티티를 @Lock 으로 다시 조회하면
	//   SQL 은 나가고 락도 잡히지만, JPA 는 이미 영속 상태인 인스턴스를 그대로
	//   돌려준다. DB 가 돌려준 최신 값은 버려진다.
	//   → 재고 검사에 옛 sold_qty 를 쓰면 오버부킹이 난다.
	// -------------------------------------------------------------------------
	@Transactional
	public void scenario4_firstLevelCacheTrap(Long roomTypeId, LocalDate targetDate) {
		log.info("### [4] 1차 캐시 함정 확인 ###");

		List<RoomInventory> first = repository
				.findByRoomTypeIdAndStayDateBetweenOrderByStayDate(roomTypeId, targetDate, targetDate);
		RoomInventory inventory = first.get(0);
		int soldBefore = inventory.getSoldQty();
		log.info("### [4-a] 최초 조회 sold_qty = {}", soldBefore);

		// 아직 아무 락도 잡지 않았으므로 외부 UPDATE 는 즉시 성공한다.
		int changed = updateFromOutside(roomTypeId, targetDate);
		log.info("### [4-b] 외부 커넥션이 sold_qty 를 +1 하고 커밋했다 ({}행)", changed);

		List<RoomInventory> locked = repository.lockForUpdate(roomTypeId, targetDate, targetDate);
		int soldAfterLock = locked.get(0).getSoldQty();
		log.info("### [4-c] @Lock 재조회 후 sold_qty = {} (같은 인스턴스인가: {})",
				soldAfterLock, locked.get(0) == inventory);

		if (soldAfterLock == soldBefore) {
			log.warn("### [4] 함정 재현됨 — SQL 은 나갔고 락도 잡혔지만 값은 옛것({})이다.", soldAfterLock);
		} else {
			log.info("### [4] 값이 갱신되었다 ({} -> {}).", soldBefore, soldAfterLock);
		}

		// [4-d] 1차 캐시를 무시하고 DB 를 다시 읽는다. 그런데 이것으로도 부족하다.
		//       MySQL 기본 격리수준은 REPEATABLE READ 이고, 락 없는 읽기(consistent read)는
		//       이 트랜잭션이 처음 읽은 시점의 스냅샷을 본다. 그래서 외부가 커밋한 값이
		//       여전히 보이지 않는다.
		entityManager.refresh(inventory);
		log.info("### [4-d] refresh() 후 sold_qty = {} "
				+ "(락 없는 읽기 = REPEATABLE READ 스냅샷. 아직 옛 값일 수 있다)",
				inventory.getSoldQty());

		// [4-e] 락 모드를 줘서 refresh 하면 현재 읽기가 될 것으로 기대했으나 아니었다.
		//       실측 결과 나간 SQL 은 `select ... where id=?` 뿐이고 for update 가 없었다.
		//       따라서 값도 여전히 스냅샷의 옛 값이다.
		//       → refresh 로는 이 상황을 되돌릴 수 없다. 첫 조회를 락 조회로 해야 한다.
		entityManager.refresh(inventory, LockModeType.PESSIMISTIC_WRITE);
		log.info("### [4-e] refresh(PESSIMISTIC_WRITE) 후 sold_qty = {} "
				+ "— 실측: 위 SQL 에 for update 가 붙지 않았다", inventory.getSoldQty());

		log.info("### [4] 결론: stale 원인은 두 겹이다. "
				+ "(1) 영속성 컨텍스트 1차 캐시가 DB 결과를 버린다. "
				+ "(2) 락 없는 읽기는 REPEATABLE READ 스냅샷을 본다. "
				+ "→ 재고를 읽는 첫 조회부터 락 조회여야 한다. 나중에 덧붙일 수 없다.");
	}

	/**
	 * 시나리오 4 가 끝난 뒤 트랜잭션 <b>밖에서</b> 실제 DB 값을 읽는다.
	 * 새 트랜잭션이므로 스냅샷이 새로 잡히고, 이것이 다툼의 여지가 없는 정답이다.
	 */
	public int readGroundTruth(Long roomTypeId, LocalDate stayDate) {
		try (Connection conn = dataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"SELECT sold_qty FROM room_inventory "
								+ "WHERE room_type_id = ? AND stay_date = ?")) {
			ps.setLong(1, roomTypeId);
			ps.setObject(2, stayDate);
			var rs = ps.executeQuery();
			return rs.next() ? rs.getInt(1) : -1;
		} catch (Exception e) {
			throw new IllegalStateException("정답 조회 실패", e);
		}
	}

	/** 스프링 트랜잭션과 무관한 별도 커넥션에서 즉시 커밋한다. */
	private int updateFromOutside(Long roomTypeId, LocalDate stayDate) {
		try (Connection conn = dataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"UPDATE room_inventory SET sold_qty = sold_qty + 1 "
								+ "WHERE room_type_id = ? AND stay_date = ?")) {
			conn.setAutoCommit(true);
			ps.setLong(1, roomTypeId);
			ps.setObject(2, stayDate);
			return ps.executeUpdate();
		} catch (Exception e) {
			throw new IllegalStateException("외부 UPDATE 실패", e);
		}
	}
}
