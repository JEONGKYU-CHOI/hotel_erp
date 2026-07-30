package io.github.jeongkyuchoi.hotel.erp.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.NightClose;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.NightCloseRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNightRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.NightCloseResult;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.NightCloseScheduler;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 야간마감 멱등성 테스트 — 핵심 기술 과제 ③ (D-004, D-026).
 *
 * <p><b>무엇을 증명하나</b> — ① 마감은 그날 밤 숙박분만 게시하고, <b>2회 실행해도 같은
 * 숙박분을 두 번 게시하지 않는다</b>(재실행 시 게시 0건, 이력 1행). ② 게시 대상은
 * CONFIRMED·CHECKED_IN 뿐이며 HOLD 는 게시되지 않는다. ③ 동시 실행에서도 정확히 한 번만
 * 마감된다 — 유니크 제약 {@code uk_night_close} 가 직렬화 지점이다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NightCloseTest {

	// 스케줄러 자동 실행(now-1일)과 겹치지 않도록 먼 미래를 쓴다.
	private static final LocalDate D0 = LocalDate.of(2030, 6, 1);
	private static final LocalDate D1 = D0.plusDays(1);
	private static final LocalDate CHECK_OUT = D0.plusDays(2); // 2박: D0, D1
	private static final BigDecimal RATE = new BigDecimal("100000.00");

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
	@Autowired private NightCloseScheduler nightCloseScheduler;
	@Autowired private NightCloseRepository nightCloseRepository;
	@Autowired private ReservationNightRepository reservationNightRepository;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("NC").name("마감 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("NCBAR").name("기본요금")
				.baseAmount(RATE).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		// 두 밤(D0, D1) 재고. 여러 예약을 담을 수 있게 넉넉히.
		for (LocalDate d : List.of(D0, D1)) {
			roomInventoryRepository.save(RoomInventory.builder()
					.tenantId(1L).roomTypeId(roomTypeId).stayDate(d)
					.totalQty(10).soldQty(0).heldQty(0)
					.build());
		}
	}

	@AfterEach
	void cleanup() {
		nightCloseRepository.deleteAll();
		reservationRepository.deleteAll();
		roomInventoryRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	/** 2박 예약을 만들어 CONFIRMED 로 확정한다(숙박분 D0, D1 생성). */
	private Long confirmedReservation(String idem) {
		Long id = reservationService.hold(new ReservationHoldCommand(
				null, "손님", "010-1234-5678", null,
				roomTypeId, ratePlanId, D0, CHECK_OUT, 2, 0, idem)).getId();
		confirmService.confirm(id);
		return id;
	}

	private Map<LocalDate, Boolean> postedByDate() {
		return reservationNightRepository.findAll().stream()
				.collect(Collectors.toMap(ReservationNight::getStayDate, ReservationNight::isPosted));
	}

	@Test
	@DisplayName("2회 실행 시 중복 게시 0 — 첫 실행만 게시, 재실행은 alreadyClosed·이력 1행")
	void closeTwice_noDoublePosting() {
		confirmedReservation("idem-nc-1");
		assertThat(postedByDate()).containsEntry(D0, false).containsEntry(D1, false);

		// 1회차 — D0 게시.
		NightCloseResult first = nightCloseScheduler.run(D0);
		assertThat(first.alreadyClosed()).isFalse();
		assertThat(first.postedNightCount()).isEqualTo(1);
		assertThat(first.postedAmount()).isEqualByComparingTo(RATE);
		assertThat(postedByDate()).as("D0 만 게시, D1 은 아직")
				.containsEntry(D0, true).containsEntry(D1, false);

		// 2회차 — 같은 영업일. 중복 게시 0.
		NightCloseResult second = nightCloseScheduler.run(D0);
		assertThat(second.alreadyClosed()).as("재실행은 멱등 no-op").isTrue();
		assertThat(second.postedNightCount()).as("재게시 0건").isZero();
		assertThat(postedByDate()).as("게시 상태 불변")
				.containsEntry(D0, true).containsEntry(D1, false);

		// 이력은 영업일당 정확히 한 행.
		List<NightClose> history = nightCloseRepository.findAll();
		assertThat(history).hasSize(1);
		assertThat(history.get(0).getBusinessDate()).isEqualTo(D0);
		assertThat(history.get(0).getPostedNightCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("HOLD 예약의 숙박분은 게시되지 않는다 (CONFIRMED·CHECKED_IN 만)")
	void hold_isNotPosted() {
		// 확정하지 않은 HOLD.
		reservationService.hold(new ReservationHoldCommand(
				null, "손님", "010-9999-8888", null,
				roomTypeId, ratePlanId, D0, CHECK_OUT, 2, 0, "idem-nc-hold"));

		NightCloseResult result = nightCloseScheduler.run(D0);
		assertThat(result.postedNightCount()).as("HOLD 는 게시 대상 아님").isZero();
		assertThat(postedByDate()).containsEntry(D0, false);
		// 이력은 남는다(빈 마감도 그 영업일이 마감됐다는 사실이다).
		assertThat(nightCloseRepository.findAll()).hasSize(1);
	}

	@RepeatedTest(8)
	@DisplayName("동시 실행 → 정확히 한 번만 마감, 이력 1행 · 게시 정확히 1건")
	void concurrentClose_exactlyOnce() throws InterruptedException {
		confirmedReservation("idem-nc-race");

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);
		AtomicInteger closedRuns = new AtomicInteger();   // 실제 마감(alreadyClosed=false) 성공 수
		AtomicInteger totalPosted = new AtomicInteger();  // 게시 건수 합
		AtomicInteger errors = new AtomicInteger();       // 유니크 충돌로 롤백된 수

		Function<Void, Void> task = v -> {
			try {
				start.await();
				NightCloseResult r = nightCloseScheduler.run(D0);
				if (!r.alreadyClosed()) {
					closedRuns.incrementAndGet();
					totalPosted.addAndGet(r.postedNightCount());
				}
			} catch (Throwable t) {
				errors.incrementAndGet(); // 경합에서 진 쪽은 uk_night_close 위반으로 롤백될 수 있다
			} finally {
				done.countDown();
			}
			return null;
		};
		pool.submit(() -> task.apply(null));
		pool.submit(() -> task.apply(null));

		start.countDown();
		done.await(30, TimeUnit.SECONDS);
		pool.shutdownNow();

		// 어느 경우든: 이력 정확히 1행, 게시 정확히 1건, D0 posted=true.
		List<NightClose> history = nightCloseRepository.findAll();
		assertThat(history).as("영업일당 이력 1행").hasSize(1);
		assertThat(history.get(0).getPostedNightCount()).isEqualTo(1);
		assertThat(postedByDate()).containsEntry(D0, true).containsEntry(D1, false);
		// 실제 게시를 수행한 실행은 최대 1개다(나머지는 alreadyClosed 이거나 롤백).
		assertThat(closedRuns.get()).as("실제 마감 성공은 1회 이하").isLessThanOrEqualTo(1);
	}
}
