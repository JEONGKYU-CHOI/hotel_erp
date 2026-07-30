package io.github.jeongkyuchoi.hotel.erp.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.Payment;
import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.PaymentRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.PaymentStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse.Settlement;
import io.github.jeongkyuchoi.hotel.erp.payment.client.TossPaymentClient;
import io.github.jeongkyuchoi.hotel.erp.payment.service.RefundService;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.NoShowService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationCancelService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 환불 실행 테스트 (D-039).
 *
 * <p><b>토스 취소 API 만 {@link MockitoBean} 으로 mock</b> 한다 — 실제 PG 호출 없이 원장 반영·
 * 부분/전액 환불·멱등·무동작을 검증한다(PaymentApiTest 와 같은 규율). 증명: ① 위약금 있는
 * 노쇼는 부분환불(남은 유효 결제액 = 위약금), ② 무료취소는 전액환불(결제 CANCELED),
 * ③ 재환불은 무동작(멱등, 토스 재호출 없음), ④ 완납이면 무동작.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RefundTest {

	private static final LocalDate D0 = LocalDate.of(2033, 6, 1);
	private static final LocalDate D2 = D0.plusDays(2); // 2박
	private static final BigDecimal PAID = new BigDecimal("200000.00");

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
	@Autowired private ReservationCancelService cancelService;
	@Autowired private NoShowService noShowService;
	@Autowired private RefundService refundService;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private PaymentRepository paymentRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	/** 토스 취소 API 만 mock. cancel() 은 void 라 성공(무동작)으로 둔다. */
	@MockitoBean private TossPaymentClient tossPaymentClient;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("REF").name("환불 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		// 환불가능 · penalty_rate 30: 노쇼 위약금 = 총액의 30%, 미래 취소는 무료.
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("REFBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("30")).active(true)
				.build()).getId();
		for (LocalDate d : java.util.List.of(D0, D0.plusDays(1))) {
			roomInventoryRepository.save(RoomInventory.builder()
					.tenantId(1L).roomTypeId(roomTypeId).stayDate(d)
					.totalQty(10).soldQty(0).heldQty(0)
					.build());
		}
	}

	@AfterEach
	void cleanup() {
		paymentRepository.deleteAll();
		reservationRepository.deleteAll();
		roomInventoryRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	/** 2박 확정 예약을 만들고 결제 20만을 기록한다. */
	private Long confirmedAndPaid(String idem, String payKey) {
		Long id = reservationService.hold(new ReservationHoldCommand(
				null, "손님", "010-1234-5678", null,
				roomTypeId, ratePlanId, D0, D2, 2, 0, idem)).getId();
		confirmService.confirm(id);
		Reservation r = reservationRepository.findById(id).orElseThrow();
		paymentRepository.save(Payment.builder()
				.tenantId(1L).reservationId(id).orderId(r.getReservationNo())
				.paymentKey(payKey).amount(PAID).status(PaymentStatus.APPROVED)
				.method("카드").approvedAt(LocalDateTime.now())
				.build());
		return id;
	}

	private Payment paymentOf(Long reservationId) {
		return paymentRepository.findByReservationIdOrderByApprovedAt(reservationId).get(0);
	}

	@Test
	@DisplayName("노쇼 위약금 30% → 부분환불(14만), 유효 결제액=위약금(6만), 잔액 0")
	void noShow_partialRefund() {
		Long id = confirmedAndPaid("ref-ns", "pk-ns");
		noShowService.markNoShows(D0); // no_show_fee = 200000 × 30% = 60000

		FolioResponse f = refundService.refund(id, "노쇼 부분환불");

		verify(tossPaymentClient).cancel(eq("pk-ns"), eq(new BigDecimal("140000.00")), any());
		Payment p = paymentOf(id);
		assertThat(p.getCanceledAmount()).isEqualByComparingTo("140000");
		assertThat(p.getStatus()).as("부분취소라 APPROVED 유지").isEqualTo(PaymentStatus.APPROVED);
		assertThat(f.balance()).isEqualByComparingTo("0");
		assertThat(f.settlement()).isEqualTo(Settlement.PAID);
	}

	@Test
	@DisplayName("무료취소 → 전액환불(20만), 결제 CANCELED, 잔액 0")
	void freeCancel_fullRefund() {
		Long id = confirmedAndPaid("ref-cxl", "pk-cxl");
		cancelService.cancel(id, "고객 변심"); // 미래라 무료취소 → 위약금 0

		FolioResponse f = refundService.refund(id, "전액환불");

		verify(tossPaymentClient).cancel(eq("pk-cxl"), eq(new BigDecimal("200000.00")), any());
		Payment p = paymentOf(id);
		assertThat(p.getCanceledAmount()).isEqualByComparingTo("200000");
		assertThat(p.getStatus()).as("전액취소라 CANCELED").isEqualTo(PaymentStatus.CANCELED);
		assertThat(f.balance()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("재환불은 무동작(멱등) — 토스 재호출 없음, 취소액 불변")
	void refund_idempotent() {
		Long id = confirmedAndPaid("ref-idem", "pk-idem");
		cancelService.cancel(id, "변심");
		refundService.refund(id, "1차");

		refundService.refund(id, "2차(재요청)");

		// 토스 취소는 정확히 한 번만.
		verify(tossPaymentClient, org.mockito.Mockito.times(1)).cancel(eq("pk-idem"), any(), any());
		assertThat(paymentOf(id).getCanceledAmount()).isEqualByComparingTo("200000");
	}

	@Test
	@DisplayName("완납 예약 → 환불 무동작(토스 미호출)")
	void paidInFull_noRefund() {
		Long id = confirmedAndPaid("ref-paid", "pk-paid"); // 청구 20만 = 결제 20만

		FolioResponse f = refundService.refund(id, "환불 시도");

		verify(tossPaymentClient, never()).cancel(any(), any(), any());
		assertThat(f.settlement()).isEqualTo(Settlement.PAID);
		assertThat(paymentOf(id).getCanceledAmount()).isEqualByComparingTo("0");
	}
}
