package io.github.jeongkyuchoi.hotel.erp.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * 당일 예약 마감 경계 테스트 (D-045).
 *
 * <p><b>무엇을 증명하나</b> — 체크인이 오늘이고 현재 시각이 마감(booking_policy 기본 20:00)을
 * 넘겼으면 HOLD 를 거절하고, 마감 전이면 받는다. "지금"에 의존하는 로직이라 실제 벽시계로는
 * CI 가 도는 시간에 따라 결과가 흔들린다 — 그래서 {@link Clock} 을 고정 시각으로 주입해
 * 경계 양쪽을 결정론적으로 재현한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SameDayCutoffTest {

	/** 체크인 당일. 재고를 이 날짜로 시드하고, 시계도 이 날짜에 고정한다. */
	private static final LocalDate TODAY = LocalDate.of(2035, 3, 15);
	private static final ZoneId ZONE = ZoneId.systemDefault();

	@TestConfiguration
	static class FixedClockConfig {
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(TODAY.atTime(19, 0).atZone(ZONE).toInstant());
		}
	}

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;
	@Autowired private MutableClock clock;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("CUT").name("당일마감 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("CUTBAR").name("조식포함")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(true).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		// 당일 1박 — 체크인 오늘, 체크아웃 내일. 두 날짜분 재고를 만든다.
		for (LocalDate d : new LocalDate[] {TODAY, TODAY.plusDays(1)}) {
			roomInventoryRepository.save(RoomInventory.builder()
					.tenantId(1L).roomTypeId(roomTypeId).stayDate(d)
					.totalQty(5).soldQty(0).heldQty(0)
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

	private Reservation holdToday(String idem) {
		return reservationService.hold(new ReservationHoldCommand(
				null, "홍길동", "010-1234-5678", "gil@example.com",
				roomTypeId, ratePlanId, TODAY, TODAY.plusDays(1), 2, 0, idem));
	}

	@Test
	@DisplayName("마감(20:00) 전 당일 예약 → HOLD 생성")
	void beforeCutoff_allowed() {
		clock.setInstant(TODAY.atTime(19, 0).atZone(ZONE).toInstant());

		Reservation r = holdToday("cut-before");

		assertThat(r.getStatus()).isEqualTo(ReservationStatus.HOLD);
	}

	@Test
	@DisplayName("마감(20:00) 후 당일 예약 → 거절(IllegalArgumentException)")
	void afterCutoff_rejected() {
		clock.setInstant(TODAY.atTime(21, 0).atZone(ZONE).toInstant());

		assertThatThrownBy(() -> holdToday("cut-after"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("당일 예약은");
	}

	@Test
	@DisplayName("마감 후라도 내일 이후 체크인은 정상 → 마감은 '당일'에만 적용")
	void afterCutoff_futureDateStillAllowed() {
		clock.setInstant(TODAY.atTime(21, 0).atZone(ZONE).toInstant());

		Reservation r = reservationService.hold(new ReservationHoldCommand(
				null, "홍길동", "010-1234-5678", "gil@example.com",
				roomTypeId, ratePlanId, TODAY.plusDays(1), TODAY.plusDays(2), 2, 0, "cut-future"));

		assertThat(r.getStatus()).isEqualTo(ReservationStatus.HOLD);
	}

	/** 테스트가 시각을 갈아끼울 수 있는 {@link Clock}. instant 만 바꾸고 존은 고정한다. */
	static class MutableClock extends Clock {
		private volatile Instant instant;

		MutableClock(Instant instant) {
			this.instant = instant;
		}

		void setInstant(Instant instant) {
			this.instant = instant;
		}

		@Override
		public Instant instant() {
			return instant;
		}

		@Override
		public ZoneId getZone() {
			return ZONE;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}
}
