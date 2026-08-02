package io.github.jeongkyuchoi.hotel.erp.backoffice;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.DashboardView;
import io.github.jeongkyuchoi.hotel.erp.backoffice.service.DashboardService;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 대시보드 실적 KPI(매출·가동률·ADR) 집계 테스트.
 *
 * <p><b>무엇을 증명하나</b> — ① 오늘 묵는 확정 예약의 숙박분이 매출·판매객실로 잡히고
 * OCC(판매/총객실)·ADR(매출/판매객실)이 맞게 계산된다. ② HOLD(미확정)는 실적에서 빠진다.
 *
 * <p>대시보드는 {@code LocalDate.now()} 기준 "오늘"을 집계하므로 예약을 오늘 밤에 묵도록
 * 만든다. 당일 체크인은 {@link ReservationService} 의 당일 마감(D-045)에 걸릴 수 있어,
 * 마감 전 시각(09:00)으로 고정한 {@link Clock} 을 주입해 언제 돌려도 통과하게 한다 —
 * 고정 시각의 날짜는 실제 오늘과 같으므로 대시보드 집계일과 어긋나지 않는다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DashboardKpiTest {

	private static final LocalDate TODAY = LocalDate.now();
	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final BigDecimal RATE = new BigDecimal("200000.00");

	@TestConfiguration
	static class FixedClockConfig {
		@Bean
		@Primary
		Clock testClock() {
			return Clock.fixed(TODAY.atTime(9, 0).atZone(ZONE).toInstant(), ZONE);
		}
	}

	@Autowired private MockMvc mockMvc;
	@Autowired private DashboardService dashboardService;
	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("KPI").name("실적 테스트 타입")
				.standardOccupancy(2).maxOccupancy(3).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("KPIBAR").name("기본요금")
				.baseAmount(RATE).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		// 오늘 밤 총 5실. 체크아웃 당일(내일)분도 재고가 있어야 hold 가 통과한다.
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

	private Reservation holdTonight(String idem) {
		return reservationService.hold(new ReservationHoldCommand(
				null, "홍길동", "010-1234-5678", "gil@example.com",
				roomTypeId, ratePlanId, TODAY, TODAY.plusDays(1), 2, 0, idem));
	}

	@Test
	@DisplayName("확정 예약 1건 → 매출·판매객실·OCC·ADR 이 맞게 잡힌다")
	void confirmedReservation_countsIntoKpi() {
		Reservation held = holdTonight("kpi-confirm");
		confirmService.confirm(held.getId());

		DashboardView v = dashboardService.load();

		assertThat(v.soldRoomsToday()).isEqualTo(1);
		assertThat(v.totalRoomsToday()).isEqualTo(5);
		assertThat(v.roomRevenueToday()).isEqualByComparingTo(RATE);
		assertThat(v.occupancyPct()).isEqualTo(20.0);          // 1/5
		assertThat(v.adrToday()).isEqualByComparingTo("200000"); // 매출/판매객실
	}

	@Test
	@DisplayName("HOLD(미확정)는 실적에서 제외된다")
	void holdReservation_excludedFromKpi() {
		holdTonight("kpi-hold-only"); // 확정하지 않음

		DashboardView v = dashboardService.load();

		assertThat(v.soldRoomsToday()).isZero();
		assertThat(v.roomRevenueToday()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(v.occupancyPct()).isEqualTo(0.0);
		assertThat(v.adrToday()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	@WithMockUser(username = "frontdesk", roles = "STAFF")
	@DisplayName("대시보드 화면이 실적 KPI 섹션까지 정상 렌더된다")
	void dashboardScreen_rendersKpiSection() throws Exception {
		Reservation held = holdTonight("kpi-render");
		confirmService.confirm(held.getId());

		mockMvc.perform(get("/admin"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("오늘 객실 매출")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("가동률 (OCC)")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ADR (평균 객실 단가)")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("₩200,000")));
	}
}
