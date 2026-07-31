package io.github.jeongkyuchoi.hotel.erp.backoffice;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 백오피스 예약 목록·상세 화면 렌더링 테스트(D-029).
 *
 * <p><b>왜 렌더링까지 검증하나</b> — OSIV 를 껐으므로({@code open-in-view: false}) 템플릿이
 * 지연로딩 연관을 건드리면 {@code LazyInitializationException} 이 난다. 목록의 fetch join,
 * 상세의 트랜잭션 내 조립이 실제로 그 예외를 막는지, 상태 배지 프래그먼트가 해석되는지는
 * 컨트롤러 호출만으로는 드러나지 않는다. MockMvc 로 HTML 을 실제로 만들어 확인한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "frontdesk", roles = "STAFF")
class ReservationAdminScreenTest {

	private static final LocalDate D0 = LocalDate.of(2033, 4, 5);

	@Autowired private MockMvc mockMvc;
	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomRepository roomRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long reservationId;
	private String reservationNo;
	private Long roomTypeId;
	private Long roomId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("SCR").name("스크린 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		Long ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("SCRBAR").name("조식포함")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(true).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		for (LocalDate d : new LocalDate[] {D0, D0.plusDays(1)}) {
			roomInventoryRepository.save(RoomInventory.builder()
					.tenantId(1L).roomTypeId(roomType.getId()).stayDate(d)
					.totalQty(5).soldQty(0).heldQty(0)
					.build());
		}
		roomId = roomRepository.save(Room.builder()
				.tenantId(1L).roomType(roomType).roomNo("201").floor((short) 2)
				.occupancyStatus(OccupancyStatus.VACANT).cleanStatus(CleanStatus.CLEAN).active(true)
				.build()).getId();
		var held = reservationService.hold(new ReservationHoldCommand(
				null, "홍길동", "010-1234-5678", "gil@example.com",
				roomType.getId(), ratePlanId, D0, D0.plusDays(2), 2, 1, "idem-scr"));
		confirmService.confirm(held.getId());
		reservationId = held.getId();
		reservationNo = held.getReservationNo();
	}

	@AfterEach
	void cleanup() {
		reservationRepository.deleteAll();
		roomInventoryRepository.deleteAll();
		roomRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	@Test
	@DisplayName("목록 화면 — 200, 예약번호·고객·타입 렌더링(지연로딩 예외 없음)")
	void listScreen_renders() throws Exception {
		mockMvc.perform(get("/admin/reservations"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/reservation/list"))
				.andExpect(model().attributeExists("reservations"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(reservationNo)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("홍길동")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("스크린 테스트 타입")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("확정"))); // 상태 배지
	}

	@Test
	@DisplayName("상세 화면 — 200, 야간 스냅샷·요금정책·취소 버튼 렌더링(트랜잭션 내 조립)")
	void detailScreen_renders() throws Exception {
		mockMvc.perform(get("/admin/reservations/{id}", reservationId))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/reservation/detail"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(reservationNo)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("조식포함")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("일자별 요금")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("100,000원")))
				// 확정 예약이라 취소 버튼이 보인다.
				.andExpect(content().string(org.hamcrest.Matchers.containsString("예약 취소")));
	}

	@Test
	@DisplayName("취소 POST → 리다이렉트, CANCELLED 전이·재고(sold) 반환")
	void cancel_transitionsAndRestoresInventory() throws Exception {
		mockMvc.perform(post("/admin/reservations/{id}/cancel", reservationId)
						.param("reason", "고객 요청")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/reservations/" + reservationId));

		var r = reservationRepository.findById(reservationId).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(r.getStatus().name()).isEqualTo("CANCELLED");
		org.assertj.core.api.Assertions.assertThat(r.getCancelReason()).isEqualTo("고객 요청");
		// 로그인 직원이 행위자로 굳는다(D-042).
		org.assertj.core.api.Assertions.assertThat(r.getCancelledBy()).isEqualTo("frontdesk");
		// 확정(sold=1)이 취소로 반환돼 0.
		var inv = roomInventoryRepository
				.findByRoomTypeIdAndStayDateBetweenOrderByStayDate(roomTypeId, D0, D0).get(0);
		org.assertj.core.api.Assertions.assertThat(inv.getSoldQty()).isZero();
	}

	@Test
	@DisplayName("취소 후 상세 → 환불·취소 이력 패널에 처리자·사유 렌더(D-042)")
	void detailScreen_afterCancel_showsHistory() throws Exception {
		mockMvc.perform(post("/admin/reservations/{id}/cancel", reservationId)
						.param("reason", "고객 요청").with(csrf()))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(get("/admin/reservations/{id}", reservationId))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("환불·취소 이력")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("고객 요청")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("frontdesk")));
	}

	@Test
	@DisplayName("체크인 POST → 리다이렉트, CHECKED_IN·호실 OCCUPIED")
	void checkIn_transitionsAndOccupiesRoom() throws Exception {
		mockMvc.perform(post("/admin/reservations/{id}/check-in", reservationId)
						.param("roomId", roomId.toString())
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/reservations/" + reservationId));

		org.assertj.core.api.Assertions.assertThat(
						reservationRepository.findById(reservationId).orElseThrow().getStatus().name())
				.isEqualTo("CHECKED_IN");
		org.assertj.core.api.Assertions.assertThat(
						roomRepository.findById(roomId).orElseThrow().getOccupancyStatus())
				.isEqualTo(OccupancyStatus.OCCUPIED);
	}
}
