package io.github.jeongkyuchoi.hotel.erp.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotEnoughInventoryException;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 오버부킹 0건 동시성 테스트 (D-006 핵심 3과제 ①, D-018·D-023 의 회귀 테스트).
 *
 * <p><b>무엇을 증명하나</b> — 재고가 1개뿐인 하룻밤에 여러 요청이 동시에 예약을 시도해도
 * 정확히 하나만 성공하고 나머지는 재고 부족으로 거부된다. {@code held_qty} 는 1을 넘지
 * 않는다. 이것이 이 프로젝트의 핵심 주장이다.
 *
 * <p><b>왜 이 테스트가 D-018 을 검증하나</b> — 각 스레드의 트랜잭션은 재고를 처음 읽는
 * 쿼리가 {@code FOR UPDATE} 다. 락을 먼저 잡으므로 뒤늦게 들어온 트랜잭션은 앞선 것이
 * 커밋한 <b>최신</b> {@code held_qty} 를 읽는다(락 읽기는 스냅샷이 아니라 최신 커밋을 본다).
 * 만약 표시용 조회를 먼저 했다면 REPEATABLE READ 스냅샷의 옛 값(0)을 읽어 전부 통과하고
 * 오버부킹이 났을 것이다. H2 로는 이 락 동작이 재현되지 않아 실제 MySQL 을 띄운다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservationConcurrencyTest {

	private static final int THREADS = 20;
	private static final LocalDate NIGHT = LocalDate.of(2030, 1, 1);

	@Autowired private ReservationService reservationService;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private ReservationRepository reservationRepository;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		// 각 save 는 자체 트랜잭션에서 커밋된다(테스트 메서드에 @Transactional 이 없다).
		// 스레드들이 시작하기 전에 데이터가 DB 에 확정돼 있어야 한다.
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("CONC").name("동시성 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();

		RatePlan ratePlan = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("CONCBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build());
		ratePlanId = ratePlan.getId();

		// ★ 재고 1개짜리 하룻밤. 이 1을 놓고 THREADS 개가 경합한다.
		roomInventoryRepository.save(RoomInventory.builder()
				.tenantId(1L).roomTypeId(roomTypeId).stayDate(NIGHT)
				.totalQty(1).soldQty(0).heldQty(0)
				.build());
	}

	@AfterEach
	void cleanup() {
		reservationRepository.deleteAll(); // reservation_night 는 cascade 로 함께 삭제
		roomInventoryRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	@Test
	@DisplayName("재고 1개, 동시 20건 → 정확히 1건 성공, 오버부킹 0건")
	void noOverbooking_underConcurrency() throws InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		CountDownLatch ready = new CountDownLatch(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(THREADS);

		AtomicInteger success = new AtomicInteger();
		AtomicInteger soldOut = new AtomicInteger();
		List<Throwable> unexpected = new CopyOnWriteArrayList<>();

		for (int i = 0; i < THREADS; i++) {
			final int n = i;
			pool.submit(() -> {
				ready.countDown();
				try {
					start.await(); // 모든 스레드를 한 지점에서 동시에 출발시킨다
					ReservationHoldCommand cmd = new ReservationHoldCommand(
							null, "손님" + n, "010-0000-00" + String.format("%02d", n), null,
							roomTypeId, ratePlanId, NIGHT, NIGHT.plusDays(1), 2, 0,
							"idem-" + n); // 서로 다른 멱등키 — 멱등 병합이 아니라 진짜 경합
					reservationService.hold(cmd);
					success.incrementAndGet();
				} catch (NotEnoughInventoryException e) {
					soldOut.incrementAndGet(); // 기대된 실패
				} catch (Throwable t) {
					unexpected.add(t); // 데드락 등 예상 못한 실패는 따로 모은다
				} finally {
					done.countDown();
				}
			});
		}

		ready.await();          // 전원 준비될 때까지 대기
		start.countDown();      // 동시 출발
		done.await(60, TimeUnit.SECONDS);
		pool.shutdownNow();

		// 예상치 못한 예외가 있으면 메시지에 실어 원인을 바로 본다
		assertThat(unexpected)
				.as("데드락 등 예상 밖 예외: %s", unexpected)
				.isEmpty();
		assertThat(success.get()).as("성공 건수").isEqualTo(1);
		assertThat(soldOut.get()).as("재고부족 거부 건수").isEqualTo(THREADS - 1);

		// DB 최종 상태: held_qty 는 정확히 1, 절대 초과 없음
		RoomInventory inv = roomInventoryRepository
				.findByRoomTypeIdAndStayDateBetweenOrderByStayDate(roomTypeId, NIGHT, NIGHT)
				.get(0);
		assertThat(inv.getHeldQty()).as("held_qty").isEqualTo(1);
		assertThat(inv.availableQty()).as("가용 재고").isZero();

		// 저장된 예약도 정확히 1건, HOLD 상태
		List<Reservation> all = reservationRepository.findAll();
		assertThat(all).hasSize(1);
		assertThat(all.get(0).getStatus()).isEqualTo(ReservationStatus.HOLD);
	}
}
