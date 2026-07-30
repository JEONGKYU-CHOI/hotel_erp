package io.github.jeongkyuchoi.hotel.erp.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.CleanStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.OccupancyStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.StayService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 체크인·체크아웃 테스트 (D-031).
 *
 * <p><b>무엇을 증명하나</b> — ① 체크인은 확정 예약에 호실을 배정하고 호실을 OCCUPIED 로,
 * 체크아웃은 CHECKED_OUT + 호실 VACANT·DIRTY 로 전이한다. ② 확정 아님·타입 불일치·배정 불가
 * 호실은 거부된다. ③ 두 예약이 같은 호실을 동시에 체크인하면 정확히 하나만 성공한다
 * (호실 행 락이 직렬화 지점, D-031).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StayTest {

	private static final LocalDate NIGHT = LocalDate.of(2034, 2, 1);

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
	@Autowired private StayService stayService;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomRepository roomRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long roomTypeId;
	private Long ratePlanId;
	private Long roomId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("STY").name("투숙 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("STYBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		roomId = roomRepository.save(Room.builder()
				.tenantId(1L).roomType(roomType).roomNo("101").floor((short) 1)
				.occupancyStatus(OccupancyStatus.VACANT).cleanStatus(CleanStatus.CLEAN).active(true)
				.build()).getId();
		// 재고는 넉넉히 — 여러 예약을 확정할 수 있게.
		roomInventoryRepository.save(RoomInventory.builder()
				.tenantId(1L).roomTypeId(roomTypeId).stayDate(NIGHT)
				.totalQty(10).soldQty(0).heldQty(0)
				.build());
	}

	@AfterEach
	void cleanup() {
		reservationRepository.deleteAll();
		roomInventoryRepository.deleteAll();
		roomRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	private Long confirmedReservation(String idem) {
		Long id = reservationService.hold(new ReservationHoldCommand(
				null, "손님", "010-1234-5678", null,
				roomTypeId, ratePlanId, NIGHT, NIGHT.plusDays(1), 2, 0, idem)).getId();
		confirmService.confirm(id);
		return id;
	}

	private Room room() {
		return roomRepository.findById(roomId).orElseThrow();
	}

	@Test
	@DisplayName("체크인 → CHECKED_IN·호실 배정·OCCUPIED, 체크아웃 → CHECKED_OUT·VACANT·DIRTY")
	void checkInThenCheckOut() {
		Long id = confirmedReservation("stay-1");

		stayService.checkIn(id, roomId);
		Reservation afterIn = reservationRepository.findById(id).orElseThrow();
		assertThat(afterIn.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
		assertThat(room().getOccupancyStatus()).isEqualTo(OccupancyStatus.OCCUPIED);

		stayService.checkOut(id);
		assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.CHECKED_OUT);
		assertThat(room().getOccupancyStatus()).isEqualTo(OccupancyStatus.VACANT);
		assertThat(room().getCleanStatus()).as("퇴실 후 청소 대상").isEqualTo(CleanStatus.DIRTY);
	}

	@Test
	@DisplayName("확정 아님(HOLD) 체크인 → 거부")
	void checkIn_notConfirmed_rejected() {
		Long holdId = reservationService.hold(new ReservationHoldCommand(
				null, "손님", "010-1234-5678", null,
				roomTypeId, ratePlanId, NIGHT, NIGHT.plusDays(1), 2, 0, "stay-hold")).getId();

		assertThatThrownBy(() -> stayService.checkIn(holdId, roomId))
				.isInstanceOf(IllegalStateException.class);
		assertThat(room().getOccupancyStatus()).as("배정 안 됨").isEqualTo(OccupancyStatus.VACANT);
	}

	@Test
	@DisplayName("이미 투숙 중인 호실 배정 → 거부(배정 불가)")
	void checkIn_occupiedRoom_rejected() {
		stayService.checkIn(confirmedReservation("stay-a"), roomId); // 101 점유
		Long other = confirmedReservation("stay-b");

		assertThatThrownBy(() -> stayService.checkIn(other, roomId))
				.isInstanceOf(IllegalStateException.class);
	}

	@RepeatedTest(8)
	@DisplayName("같은 호실 동시 체크인 → 정확히 하나만 성공")
	void concurrentCheckIn_sameRoom_oneWins() throws InterruptedException {
		Long a = confirmedReservation("stay-race-a");
		Long b = confirmedReservation("stay-race-b");

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);
		AtomicInteger ok = new AtomicInteger();

		Runnable taskA = () -> runCheckIn(a, start, done, ok);
		Runnable taskB = () -> runCheckIn(b, start, done, ok);
		pool.submit(taskA);
		pool.submit(taskB);

		start.countDown();
		done.await(30, TimeUnit.SECONDS);
		pool.shutdownNow();

		assertThat(ok.get()).as("정확히 하나만 배정 성공").isEqualTo(1);
		assertThat(room().getOccupancyStatus()).isEqualTo(OccupancyStatus.OCCUPIED);
		long checkedIn = java.util.stream.Stream.of(a, b)
				.map(id -> reservationRepository.findById(id).orElseThrow().getStatus())
				.filter(s -> s == ReservationStatus.CHECKED_IN)
				.count();
		assertThat(checkedIn).as("CHECKED_IN 예약은 하나").isEqualTo(1);
	}

	private void runCheckIn(Long id, CountDownLatch start, CountDownLatch done, AtomicInteger ok) {
		try {
			start.await();
			stayService.checkIn(id, roomId);
			ok.incrementAndGet();
		} catch (Throwable ignored) {
			// 경합에서 진 쪽은 배정 불가로 거부된다.
		} finally {
			done.countDown();
		}
	}
}
