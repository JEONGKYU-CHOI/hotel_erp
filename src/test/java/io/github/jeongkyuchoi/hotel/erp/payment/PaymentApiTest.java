package io.github.jeongkyuchoi.hotel.erp.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.PaymentRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.payment.client.TossConfirmResponse;
import io.github.jeongkyuchoi.hotel.erp.payment.client.TossPaymentClient;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 결제 REST 통합테스트(D-034). 승인 → 확정 배선과 위변조 방어·멱등·만료 경합·웹훅 정합을 검증한다.
 *
 * <p>실제 MySQL(Testcontainers) 위에서 컨트롤러·서비스·확정·영속까지 통으로 돈다.
 * <b>토스 승인 API 만 {@link MockitoBean} 으로 mock</b> 한다 — 실제 결제창·PG 호출 없이 서버
 * 로직(금액 대조·확정 전이·결제 기록·멱등)을 검증하는 게 목적이다(핸드오프 방침). 요청 JSON 은
 * 계약을 또렷이 드러내려 손으로 쓴다(BookingApiTest 와 같은 규율).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PaymentApiTest {

	private static final LocalDate NIGHT = LocalDate.of(2030, 5, 1);
	private static final BigDecimal AMOUNT = new BigDecimal("100000.00"); // 1박 요금
	private static final String PHONE = "010-1234-5678";
	private static final String PAYMENT_KEY = "test_paymentKey_abc123";

	@Autowired private MockMvc mockMvc;
	@Autowired private ReservationService reservationService;
	@Autowired private HoldExpiryScheduler holdExpiryScheduler;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;
	@Autowired private PaymentRepository paymentRepository;

	/** 토스 승인 API 만 mock. 나머지는 실제 빈으로 돈다. */
	@MockitoBean private TossPaymentClient tossPaymentClient;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("PAY").name("결제 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("PAYBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		roomInventoryRepository.save(RoomInventory.builder()
				.tenantId(1L).roomTypeId(roomTypeId).stayDate(NIGHT)
				.totalQty(1).soldQty(0).heldQty(0)
				.build());
	}

	@AfterEach
	void cleanup() {
		paymentRepository.deleteAll();
		reservationRepository.deleteAll();
		roomInventoryRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	/** HOLD 하나 만들고 예약번호를 돌려준다(총액은 항상 AMOUNT). */
	private String createHold(String idem) {
		Reservation r = reservationService.hold(new ReservationHoldCommand(
				null, "손님", PHONE, null,
				roomTypeId, ratePlanId, NIGHT, NIGHT.plusDays(1), 2, 0, idem));
		return r.getReservationNo();
	}

	private RoomInventory inventory() {
		return roomInventoryRepository
				.findByRoomTypeIdAndStayDateBetweenOrderByStayDate(roomTypeId, NIGHT, NIGHT)
				.get(0);
	}

	private ReservationStatus statusOf(String no) {
		return reservationRepository.findByTenantIdAndReservationNo(1L, no).orElseThrow().getStatus();
	}

	/** 토스가 정상 승인했다고 응답하도록 mock. 금액은 인자로(응답 위조 케이스용). */
	private void givenTossApproves(BigDecimal respondAmount) {
		given(tossPaymentClient.confirm(anyString(), anyString(), any()))
				.willAnswer(inv -> new TossConfirmResponse(
						inv.getArgument(0), inv.getArgument(1), respondAmount,
						"DONE", "간편결제", "2030-05-01T12:00:00+09:00"));
	}

	private String confirmJson(String paymentKey, String orderId, String amount) {
		return """
				{ "paymentKey": "%s", "orderId": "%s", "amount": %s }
				""".formatted(paymentKey, orderId, amount);
	}

	@Test
	@DisplayName("정상 승인 → 200 APPROVED, 예약 CONFIRMED, held→sold, 결제 1건")
	void confirm_success() throws Exception {
		String no = createHold("pay-ok");
		givenTossApproves(AMOUNT);

		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(confirmJson(PAYMENT_KEY, no, "100000")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value(no))
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.amount").value(100000));

		assertThat(statusOf(no)).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(inventory().getHeldQty()).as("held 반환").isZero();
		assertThat(inventory().getSoldQty()).as("sold 증가").isEqualTo(1);
		assertThat(paymentRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("금액 위변조(요청<저장) → 400 AMOUNT_MISMATCH, 토스 미호출, HOLD 유지")
	void confirm_amountTampered_rejectedBeforeToss() throws Exception {
		String no = createHold("pay-tamper");

		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(confirmJson(PAYMENT_KEY, no, "50000")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));

		verify(tossPaymentClient, never()).confirm(anyString(), anyString(), any());
		assertThat(statusOf(no)).isEqualTo(ReservationStatus.HOLD);
		assertThat(paymentRepository.count()).isZero();
	}

	@Test
	@DisplayName("승인 응답 금액이 저장액과 다름 → 400 AMOUNT_MISMATCH, HOLD 유지")
	void confirm_responseAmountForged_rejected() throws Exception {
		String no = createHold("pay-forge");
		givenTossApproves(new BigDecimal("1.00")); // 응답만 위조된 금액

		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(confirmJson(PAYMENT_KEY, no, "100000")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));

		assertThat(statusOf(no)).isEqualTo(ReservationStatus.HOLD);
		assertThat(paymentRepository.count()).isZero();
	}

	@Test
	@DisplayName("멱등 — 같은 paymentKey 재요청, 토스 1회만 호출·결제 1건·이중 이동 없음")
	void confirm_idempotent() throws Exception {
		String no = createHold("pay-idem");
		givenTossApproves(AMOUNT);

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/api/payments/confirm")
							.contentType(MediaType.APPLICATION_JSON)
							.content(confirmJson(PAYMENT_KEY, no, "100000")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("APPROVED"));
		}

		verify(tossPaymentClient, times(1)).confirm(anyString(), anyString(), any());
		assertThat(paymentRepository.count()).as("결제 1건").isEqualTo(1);
		assertThat(inventory().getSoldQty()).as("이중 이동 없음").isEqualTo(1);
	}

	@Test
	@DisplayName("만료 뒤 도착한 결제 → 409 INVALID_STATE, 확정 안 됨, 결제 0건")
	void confirm_afterExpiry_conflict() throws Exception {
		String no = createHold("pay-expired");
		// HOLD 를 만료시킨다(스케줄러가 미래 시점 기준으로 훑음).
		holdExpiryScheduler.sweep(LocalDateTime.now().plusHours(1));
		assertThat(statusOf(no)).isEqualTo(ReservationStatus.EXPIRED);
		givenTossApproves(AMOUNT);

		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(confirmJson(PAYMENT_KEY, no, "100000")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATE"));

		assertThat(statusOf(no)).isEqualTo(ReservationStatus.EXPIRED);
		assertThat(paymentRepository.count()).isZero();
	}

	@Test
	@DisplayName("웹훅 DONE → 예약 확정·결제 기록, 재전송해도 결제 1건(멱등)")
	void webhook_reconcilesAndIsIdempotent() throws Exception {
		String no = createHold("pay-webhook");
		String body = """
				{ "eventType": "PAYMENT_STATUS_CHANGED",
				  "data": { "paymentKey": "%s", "orderId": "%s",
				            "status": "DONE", "totalAmount": 100000 } }
				""".formatted(PAYMENT_KEY, no);

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/api/payments/webhook")
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isOk());
		}

		assertThat(statusOf(no)).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(paymentRepository.count()).as("웹훅 재전송에도 결제 1건").isEqualTo(1);
		assertThat(inventory().getSoldQty()).isEqualTo(1);
	}
}
