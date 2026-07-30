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
import io.github.jeongkyuchoi.hotel.erp.reservation.service.HoldExpiryScheduler;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationCancelService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 예약 취소 테스트 (D-027).
 *
 * <p><b>무엇을 증명하나</b> — ① HOLD 취소는 {@code held_qty} 를, CONFIRMED 취소는
 * {@code sold_qty} 를 되돌린다(취소 직전 상태가 버킷을 결정). ② 재취소는 멱등 — 재고를
 * 두 번 되돌리지 않는다. ③ 취소 불가 상태(EXPIRED)는 거부된다. ④ 동시 이중 취소에서도
 * 재고가 정확히 한 번만 반환된다(예약 행 락이 직렬화 지점, D-025).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservationCancelTest {

	private static final LocalDate NIGHT = LocalDate.of(2031, 3, 1);

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
	@Autowired private ReservationCancelService cancelService;
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
				.tenantId(1L).code("CXL").name("취소 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("CXLBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		roomInventoryRepository.save(RoomInventory.builder()
				.tenantId(1L).roomTypeId(roomTypeId).stayDate(NIGHT)
				.totalQty(5).soldQty(0).heldQty(0)
				.build());
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
				roomTypeId, ratePlanId, NIGHT, NIGHT.plusDays(1), 2, 0, idem)).getId();
	}

	private RoomInventory inventory() {
		return roomInventoryRepository
				.findByRoomTypeIdAndStayDateBetweenOrderByStayDate(roomTypeId, NIGHT, NIGHT)
				.get(0);
	}

	@Test
	@DisplayName("HOLD 취소 → held 반환, CANCELLED·취소 메타 기록")
	void cancelHold_releasesHeld() {
		Long id = createHold("idem-cxl-hold");
		assertThat(inventory().getHeldQty()).isEqualTo(1);

		cancelService.cancel(id, "고객 변심");

		Reservation r = reservationRepository.findById(id).orElseThrow();
		assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
		assertThat(r.getCancelledAt()).isNotNull();
		assertThat(r.getCancelReason()).isEqualTo("고객 변심");
		assertThat(inventory().getHeldQty()).as("held 반환").isZero();
		assertThat(inventory().getSoldQty()).isZero();
	}

	@Test
	@DisplayName("CONFIRMED 취소 → sold 반환, 재취소는 멱등(이중 반환 없음)")
	void cancelConfirmed_releasesSold_andIdempotent() {
		Long id = createHold("idem-cxl-cfm");
		confirmService.confirm(id);
		assertThat(inventory().getSoldQty()).isEqualTo(1);
		assertThat(inventory().getHeldQty()).isZero();

		cancelService.cancel(id, "결제 취소");
		assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.CANCELLED);
		assertThat(inventory().getSoldQty()).as("sold 반환").isZero();

		// 재취소 — 재고를 또 되돌리지 않는다.
		cancelService.cancel(id, "중복 요청");
		assertThat(inventory().getSoldQty()).as("이중 반환 없음").isZero();
	}

	@Test
	@DisplayName("취소 불가 상태(EXPIRED)는 거부된다")
	void cancelExpired_isRejected() {
		Long id = createHold("idem-cxl-exp");
		// 미래 시각으로 청소해 EXPIRED 로 만든다.
		holdExpiryScheduler.sweep(LocalDateTime.now().plusHours(1));
		assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.EXPIRED);

		assertThatThrownBy(() -> cancelService.cancel(id, "이미 만료"))
				.isInstanceOf(IllegalStateException.class);
		// 만료가 이미 반환했으므로 재고는 그대로 0.
		assertThat(inventory().getHeldQty()).isZero();
	}

	@RepeatedTest(8)
	@DisplayName("동시 이중 취소 → 재고 정확히 한 번만 반환")
	void concurrentDoubleCancel_releasesExactlyOnce() throws InterruptedException {
		Long id = createHold("idem-cxl-race");
		confirmService.confirm(id); // sold=1 로 만들어 두 스레드가 이 1을 두고 취소 경합
		assertThat(inventory().getSoldQty()).isEqualTo(1);

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);
		AtomicInteger errors = new AtomicInteger();

		Runnable task = () -> {
			try {
				start.await();
				cancelService.cancel(id, "경합 취소");
			} catch (Throwable t) {
				errors.incrementAndGet();
			} finally {
				done.countDown();
			}
		};
		pool.submit(task);
		pool.submit(task);

		start.countDown();
		done.await(30, TimeUnit.SECONDS);
		pool.shutdownNow();

		// 두 취소 모두 멱등이라 예외 없이 통과할 수 있다. 핵심은 재고가 정확히 한 번만 반환됨.
		assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.CANCELLED);
		assertThat(inventory().getSoldQty()).as("이중 반환 없음 — sold 는 0").isZero();
		assertThat(inventory().getHeldQty()).isZero();
	}
}
