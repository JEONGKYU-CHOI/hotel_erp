package io.github.jeongkyuchoi.hotel.erp.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 회원 예약 연결 통합테스트 (D-032). 로그인 회원의 HOLD 가 회원에 연결되고
 * {@code GET /api/me/reservations} 에 뜨는지, 비회원 예약은 섞이지 않는지 검증한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MemberReservationTest {

	private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);
	private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(1);
	private static final String EMAIL = "gil@example.com";
	private static final String PASSWORD = "password123";

	@Autowired private MockMvc mockMvc;
	@Autowired private MemberRepository memberRepository;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("MEM").name("회원 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("MEMBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		roomInventoryRepository.save(RoomInventory.builder()
				.tenantId(1L).roomTypeId(roomTypeId).stayDate(CHECK_IN)
				.totalQty(5).soldQty(0).heldQty(0)
				.build());
	}

	@AfterEach
	void cleanup() {
		reservationRepository.deleteAll();
		roomInventoryRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
		memberRepository.deleteAll();
	}

	private String token() throws Exception {
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s","name":"홍길동","phone":"010-1234-5678"}
								""".formatted(EMAIL, PASSWORD)))
				.andExpect(status().isCreated());
		String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(EMAIL, PASSWORD)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private String holdJson(String idem) {
		return """
				{ "guestName":"홍길동", "guestPhone":"010-1234-5678", "guestEmail":"gil@example.com",
				  "roomTypeId":%d, "ratePlanId":%d, "checkInDate":"%s", "checkOutDate":"%s",
				  "adults":2, "children":0, "idempotencyKey":"%s" }
				""".formatted(roomTypeId, ratePlanId, CHECK_IN, CHECK_OUT, idem);
	}

	@Test
	@DisplayName("토큰으로 HOLD → 회원 연결, /api/me/reservations 에 뜸. 비회원 예약은 안 섞임")
	void memberHold_linkedAndListed() throws Exception {
		String token = token();

		// 회원 예약 (토큰 있음)
		String body = mockMvc.perform(post("/api/reservations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdJson("mem-hold")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String memberNo = JsonPath.read(body, "$.reservationNo");

		// 비회원 예약 (토큰 없음) — 회원 목록에 섞이면 안 된다.
		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdJson("anon-hold")))
				.andExpect(status().isCreated());

		// 회원 예약 목록 — 정확히 회원 것 하나만.
		mockMvc.perform(get("/api/me/reservations").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].reservationNo").value(memberNo))
				.andExpect(jsonPath("$[0].status").value("HOLD"))
				.andExpect(jsonPath("$[0].roomTypeName").value("회원 테스트 타입"))
				.andExpect(jsonPath("$[0].totalAmount").value(100000));
	}

	@Test
	@DisplayName("무토큰 /api/me/reservations → 401")
	void myReservations_noToken_unauthorized() throws Exception {
		mockMvc.perform(get("/api/me/reservations"))
				.andExpect(status().isUnauthorized());
	}
}
