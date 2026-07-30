package io.github.jeongkyuchoi.hotel.erp.folio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse.Settlement;
import io.github.jeongkyuchoi.hotel.erp.folio.service.FolioService;
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

/**
 * 폴리오(청구서) 조립 테스트 (D-038).
 *
 * <p><b>무엇을 증명하나</b> — 상태별 청구액 규칙과 잔액/정산상태: ① 확정 완납(PAID),
 * ② 취소 후 환불대상(REFUND_DUE), ③ 노쇼 위약금 완납(PAID), ④ HOLD 미수(OUTSTANDING),
 * ⑤ 회원 소유 검증(남의/비회원 예약 거부).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FolioTest {

	// 스케줄러 자동 실행과 겹치지 않게 먼 미래.
	private static final LocalDate D0 = LocalDate.of(2032, 5, 1);
	private static final LocalDate D1 = D0.plusDays(1);
	private static final BigDecimal RATE = new BigDecimal("100000.00");

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
	@Autowired private ReservationCancelService cancelService;
	@Autowired private NoShowService noShowService;
	@Autowired private FolioService folioService;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private PaymentRepository paymentRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("FOL").name("폴리오 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		// 환불가능 · penalty_rate 100: 미래 취소는 무료(기한 전), 노쇼는 전액 위약금.
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("FOLBAR").name("기본요금")
				.baseAmount(RATE).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("100")).active(true)
				.build()).getId();
		for (LocalDate d : java.util.List.of(D0, D1)) {
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

	private Long hold(LocalDate in, LocalDate out, String idem) {
		return reservationService.hold(new ReservationHoldCommand(
				null, "손님", "010-1234-5678", null,
				roomTypeId, ratePlanId, in, out, 2, 0, idem)).getId();
	}

	/** 예약에 승인 결제 한 건을 기록한다(토스 흐름 대신 직접 — 폴리오는 payment 테이블만 읽는다). */
	private void pay(Long reservationId, BigDecimal amount, String key) {
		Reservation r = reservationRepository.findById(reservationId).orElseThrow();
		paymentRepository.save(Payment.builder()
				.tenantId(1L).reservationId(reservationId).orderId(r.getReservationNo())
				.paymentKey(key).amount(amount).status(PaymentStatus.APPROVED)
				.method("카드").approvedAt(LocalDateTime.now())
				.build());
	}

	@Test
	@DisplayName("확정 2박·결제 20만 → 청구 20만·결제 20만·잔액 0·PAID")
	void confirmed_paidInFull() {
		Long id = hold(D0, D1.plusDays(1), "fol-paid"); // 2박
		confirmService.confirm(id);
		pay(id, new BigDecimal("200000.00"), "pk-paid");

		FolioResponse f = folioService.forAdmin(id);

		assertThat(f.charges()).hasSize(2); // 숙박료 일자별
		assertThat(f.chargeTotal()).isEqualByComparingTo("200000");
		assertThat(f.creditTotal()).isEqualByComparingTo("200000");
		assertThat(f.balance()).isEqualByComparingTo("0");
		assertThat(f.settlement()).isEqualTo(Settlement.PAID);
	}

	@Test
	@DisplayName("확정 후 무료취소·결제 20만 → 청구 0·잔액 −20만·REFUND_DUE")
	void cancelledFree_refundDue() {
		Long id = hold(D0, D1.plusDays(1), "fol-cxl"); // 2박
		confirmService.confirm(id);
		pay(id, new BigDecimal("200000.00"), "pk-cxl");
		cancelService.cancel(id, "고객 변심"); // 미래라 무료취소 → 위약금 0

		FolioResponse f = folioService.forAdmin(id);

		assertThat(f.charges()).as("위약금 0 이면 청구 라인 없음").isEmpty();
		assertThat(f.chargeTotal()).isEqualByComparingTo("0");
		assertThat(f.creditTotal()).isEqualByComparingTo("200000");
		assertThat(f.balance()).isEqualByComparingTo("-200000");
		assertThat(f.settlement()).isEqualTo(Settlement.REFUND_DUE);
	}

	@Test
	@DisplayName("노쇼(위약금 100%)·결제 20만 → 청구 20만·잔액 0·PAID")
	void noShow_paidPenalty() {
		Long id = hold(D0, D1.plusDays(1), "fol-ns"); // 2박, 도착 D0
		confirmService.confirm(id);
		pay(id, new BigDecimal("200000.00"), "pk-ns");
		noShowService.markNoShows(D0); // penalty_rate 100 → no_show_fee = 총액

		FolioResponse f = folioService.forAdmin(id);

		assertThat(f.charges()).hasSize(1);
		assertThat(f.charges().get(0).label()).isEqualTo("노쇼 위약금");
		assertThat(f.chargeTotal()).isEqualByComparingTo("200000");
		assertThat(f.balance()).isEqualByComparingTo("0");
		assertThat(f.settlement()).isEqualTo(Settlement.PAID);
	}

	@Test
	@DisplayName("HOLD 미결제 → 청구 숙박료·결제 0·OUTSTANDING")
	void hold_outstanding() {
		Long id = hold(D0, D1, "fol-hold"); // 1박, 미결제

		FolioResponse f = folioService.forAdmin(id);

		assertThat(f.chargeTotal()).isEqualByComparingTo("100000");
		assertThat(f.creditTotal()).isEqualByComparingTo("0");
		assertThat(f.balance()).isEqualByComparingTo("100000");
		assertThat(f.settlement()).isEqualTo(Settlement.OUTSTANDING);
	}

	@Test
	@DisplayName("소유 검증 — 비회원(member 없음) 예약을 회원 청구서로 조회하면 거부")
	void forMember_rejectsNonOwned() {
		Long id = hold(D0, D1, "fol-owner"); // memberId null → 비회원 예약
		String no = reservationRepository.findById(id).orElseThrow().getReservationNo();

		assertThatThrownBy(() -> folioService.forMember(999L, no))
				.isInstanceOf(NotFoundException.class);
	}
}
