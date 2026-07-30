package io.github.jeongkyuchoi.hotel.erp.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.NightCloseRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNightRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.NightCloseScheduler;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.NoShowService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * NO_SHOW 판정 테스트 (D-036). 야간마감 뒤 도착일 미투숙 확정 예약이 NO_SHOW 로 전이되고,
 * 도착 첫날은 과금(게시)되며, 다른 날 도착은 건드리지 않고, 재실행이 멱등임을 검증한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NoShowTest {

	// 스케줄러 자동 실행(now-1일)과 겹치지 않게 먼 미래.
	private static final LocalDate D0 = LocalDate.of(2030, 7, 1);
	private static final LocalDate D1 = D0.plusDays(1);
	private static final BigDecimal RATE = new BigDecimal("100000.00");

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
	@Autowired private NightCloseScheduler nightCloseScheduler;
	@Autowired private NoShowService noShowService;
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
				.tenantId(1L).code("NS").name("노쇼 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("NSBAR").name("기본요금")
				.baseAmount(RATE).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
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

	/** 주어진 도착일에 1박 확정 예약을 만든다. */
	private Long confirmedArrival(LocalDate arrival, String idem) {
		Long id = reservationService.hold(new ReservationHoldCommand(
				null, "손님", "010-1234-5678", null,
				roomTypeId, ratePlanId, arrival, arrival.plusDays(1), 2, 0, idem)).getId();
		confirmService.confirm(id);
		return id;
	}

	private ReservationStatus statusOf(Long id) {
		return reservationRepository.findById(id).orElseThrow().getStatus();
	}

	@Test
	@DisplayName("도착일 미투숙 CONFIRMED → 마감 후 NO_SHOW, 첫날은 게시(과금), 다른 날 도착은 그대로")
	void noShow_afterClose_marksArrivalDayConfirmed() {
		Long todayArrival = confirmedArrival(D0, "ns-today"); // D0 도착
		Long tomorrowArrival = confirmedArrival(D1, "ns-tomorrow"); // D1 도착 — 대상 아님

		nightCloseScheduler.run(D0);

		// D0 도착 확정분은 NO_SHOW, D1 도착분은 여전히 CONFIRMED.
		assertThat(statusOf(todayArrival)).isEqualTo(ReservationStatus.NO_SHOW);
		assertThat(statusOf(tomorrowArrival)).as("다른 날 도착은 판정 대상 아님")
				.isEqualTo(ReservationStatus.CONFIRMED);

		// 노쇼 첫날(D0)은 게시(과금)됐다 — 마감이 NO_SHOW 전이 전에 돌기 때문.
		var postedByDate = reservationNightRepository.findAll().stream()
				.filter(n -> n.getStayDate().equals(D0))
				.collect(Collectors.toMap(n -> n.getStayDate(), ReservationNight::isPosted, (a, b) -> a || b));
		assertThat(postedByDate).containsEntry(D0, true);
	}

	@Test
	@DisplayName("재실행 멱등 — 이미 NO_SHOW 는 다시 판정하지 않는다(0건, 오류 없음)")
	void noShow_idempotent() {
		Long id = confirmedArrival(D0, "ns-idem");

		int first = noShowService.markNoShows(D0);
		int second = noShowService.markNoShows(D0);

		assertThat(first).isEqualTo(1);
		assertThat(second).as("재실행은 대상 0건").isZero();
		assertThat(statusOf(id)).isEqualTo(ReservationStatus.NO_SHOW);
	}
}
