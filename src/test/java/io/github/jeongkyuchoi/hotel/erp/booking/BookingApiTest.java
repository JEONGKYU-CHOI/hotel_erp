package io.github.jeongkyuchoi.hotel.erp.booking;

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
 * 부킹엔진 REST 통합테스트(D-030). 가용 조회 → HOLD → 조회 흐름과 오류 매핑을 검증한다.
 *
 * <p>실제 MySQL(Testcontainers) 위에서 컨트롤러·서비스·JSON 직렬화까지 통으로 돈다.
 * 요청 JSON 은 문자열로 직접 만든다 — Boot 4 테스트 컨텍스트에서 {@code ObjectMapper} 를
 * 빈으로 주입받지 못하고, 손으로 쓴 JSON 이 계약(필드명)을 더 또렷이 드러낸다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

	private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);
	private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(2); // 2박
	private static final String PHONE = "010-1234-5678";

	@Autowired private MockMvc mockMvc;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("API").name("API 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("APIBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		for (LocalDate d : new LocalDate[] {CHECK_IN, CHECK_IN.plusDays(1)}) {
			roomInventoryRepository.save(RoomInventory.builder()
					.tenantId(1L).roomTypeId(roomTypeId).stayDate(d)
					.totalQty(1).soldQty(0).heldQty(0) // 재고 1개 — 두 번째 HOLD 는 부족
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

	/** HOLD 요청 JSON. guestName 을 비우면 검증 실패 케이스가 된다. */
	private String holdJson(String guestName, String idem) {
		return """
				{
				  "guestName": "%s",
				  "guestPhone": "%s",
				  "guestEmail": "gil@example.com",
				  "roomTypeId": %d,
				  "ratePlanId": %d,
				  "checkInDate": "%s",
				  "checkOutDate": "%s",
				  "adults": 2,
				  "children": 0,
				  "idempotencyKey": "%s"
				}
				""".formatted(guestName, PHONE, roomTypeId, ratePlanId, CHECK_IN, CHECK_OUT, idem);
	}

	@Test
	@DisplayName("GET /api/availability → 200, 일자별 가용·예약가능수")
	void availability_returnsPerNight() throws Exception {
		mockMvc.perform(get("/api/availability")
						.param("roomTypeId", roomTypeId.toString())
						.param("checkIn", CHECK_IN.toString())
						.param("checkOut", CHECK_OUT.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bookableQty").value(1))
				.andExpect(jsonPath("$.nightCount").value(2))
				.andExpect(jsonPath("$.nights.length()").value(2))
				.andExpect(jsonPath("$.nights[0].availableQty").value(1));
	}

	@Test
	@DisplayName("POST /api/reservations → 201, 예약번호·HOLD·총액")
	void hold_created() throws Exception {
		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdJson("홍길동", "api-hold-1")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reservationNo").isNotEmpty())
				.andExpect(jsonPath("$.status").value("HOLD"))
				.andExpect(jsonPath("$.nightCount").value(2))
				.andExpect(jsonPath("$.totalAmount").value(200000))
				.andExpect(jsonPath("$.holdExpiresAt").isNotEmpty());
	}

	@Test
	@DisplayName("POST 검증 실패(이름 없음) → 400 VALIDATION + 필드 메시지")
	void hold_validationError() throws Exception {
		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdJson("", "api-bad")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION"))
				.andExpect(jsonPath("$.fields.guestName").isNotEmpty());
	}

	@Test
	@DisplayName("재고 부족 HOLD → 409 NO_INVENTORY (경합의 정상 결과)")
	void hold_noInventory_conflict() throws Exception {
		// 재고 1개를 첫 HOLD 가 가져간다.
		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdJson("홍길동", "api-first")))
				.andExpect(status().isCreated());
		// 두 번째는 가용 0 → 409.
		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdJson("김철수", "api-second")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NO_INVENTORY"));
	}

	@Test
	@DisplayName("GET /api/reservations/{no} → 200(전화 일치) / 404(불일치)")
	void lookup_ownershipByPhone() throws Exception {
		String body = mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdJson("홍길동", "api-lookup")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String no = JsonPath.read(body, "$.reservationNo");

		mockMvc.perform(get("/api/reservations/{no}", no).param("phone", PHONE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reservationNo").value(no))
				.andExpect(jsonPath("$.status").value("HOLD"))
				.andExpect(jsonPath("$.nightViews.length()").value(2));

		mockMvc.perform(get("/api/reservations/{no}", no).param("phone", "010-0000-0000"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}
}
