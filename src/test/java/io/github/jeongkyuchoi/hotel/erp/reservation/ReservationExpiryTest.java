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
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.HoldExpiryScheduler;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * HOLD 만료 → 재고 복원 통합테스트 (D-006 핵심 3과제 ②, D-003 의 회귀 테스트).
 *
 * <p><b>무엇을 증명하나</b> — 만료된 HOLD 를 스케줄러가 정리하면 예약이 EXPIRED 로
 * 전이되고, 잡고 있던 {@code held_qty} 가 야간마다 정확히 반환된다. 두 번 돌려도(멱등)
 * 재고가 이중으로 반환되지 않고, 아직 만료되지 않은 HOLD 는 건드리지 않는다.
 *
 * <p><b>시각을 어떻게 결정적으로 다루나</b> — HOLD 는 {@code now + 10분} 만료다.
 * 데이터를 백데이트하는 대신 {@code sweep(미래시각)} 을 호출해 "그 시점에는 만료됨"을
 * 만든다. {@link HoldExpiryScheduler#sweep}과 {@code isHoldExpired} 가 같은 시각을
 * 쓰므로 실제 시계에 의존하지 않고 재현 가능하다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservationExpiryTest {

	private static final LocalDate CHECK_IN = LocalDate.of(2030, 3, 1);
	private static final LocalDate CHECK_OUT = LocalDate.of(2030, 3, 3); // 2박: 3/1, 3/2
	private static final int TOTAL = 5;

	@Autowired private ReservationService reservationService;
	@Autowired private HoldExpiryScheduler holdExpiryScheduler;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("EXP").name("만료 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();

		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("EXPBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();

		// 2박 각각 재고 행. total=5, 처음엔 sold=held=0.
		for (LocalDate d = CHECK_IN; d.isBefore(CHECK_OUT); d = d.plusDays(1)) {
			roomInventoryRepository.save(RoomInventory.builder()
					.tenantId(1L).roomTypeId(roomTypeId).stayDate(d)
					.totalQty(TOTAL).soldQty(0).heldQty(0)
					.build());
		}
	}

	@AfterEach
	void cleanup() {
		reservationRepository.deleteAll();
		roomInventoryRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	private Long createHold(String idem) {
		return reservationService.hold(new ReservationHoldCommand(
				null, "손님", "010-1234-5678", null,
				roomTypeId, ratePlanId, CHECK_IN, CHECK_OUT, 2, 0, idem)).getId();
	}

	private int heldOn(LocalDate date) {
		return roomInventoryRepository
				.findByRoomTypeIdAndStayDateBetweenOrderByStayDate(roomTypeId, date, date)
				.get(0).getHeldQty();
	}

	@Test
	@DisplayName("만료된 HOLD → EXPIRED 전이 + 두 야간 재고 모두 반환, 재실행 멱등")
	void expiredHold_restoresInventoryOnEveryNight() {
		Long id = createHold("idem-expire");
		// 점유 직후: 두 야간 모두 held=1
		assertThat(heldOn(CHECK_IN)).isEqualTo(1);
		assertThat(heldOn(CHECK_IN.plusDays(1))).isEqualTo(1);

		// 만료 시점을 지난 미래로 청소 실행
		LocalDateTime future = LocalDateTime.now().plusHours(1);
		int processed = holdExpiryScheduler.sweep(future);

		assertThat(processed).as("정리된 건수").isEqualTo(1);
		Reservation after = reservationRepository.findById(id).orElseThrow();
		assertThat(after.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
		assertThat(heldOn(CHECK_IN)).as("3/1 held 반환").isZero();
		assertThat(heldOn(CHECK_IN.plusDays(1))).as("3/2 held 반환").isZero();

		// 멱등: 다시 돌려도 이미 EXPIRED 라 건드리지 않는다. 재고 이중 반환 없음.
		int again = holdExpiryScheduler.sweep(future);
		assertThat(again).as("재실행 처리 건수").isZero();
		assertThat(heldOn(CHECK_IN)).isZero();
		assertThat(heldOn(CHECK_IN.plusDays(1))).isZero();
	}

	@Test
	@DisplayName("아직 만료 전 HOLD 는 청소가 건드리지 않는다")
	void activeHold_isLeftUntouched() {
		Long id = createHold("idem-active");

		// 현재 시각으로 청소 — HOLD 만료는 now+10분이라 아직 유효하다.
		int processed = holdExpiryScheduler.sweep(LocalDateTime.now());

		assertThat(processed).as("정리된 건수").isZero();
		assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.HOLD);
		assertThat(heldOn(CHECK_IN)).isEqualTo(1);
		assertThat(heldOn(CHECK_IN.plusDays(1))).isEqualTo(1);
	}
}
