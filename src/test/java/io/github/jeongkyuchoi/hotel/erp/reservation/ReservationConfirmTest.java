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
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationConfirmService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * HOLD → CONFIRMED 확정 + 확정/만료 경합 테스트 (D-025).
 *
 * <p><b>무엇을 증명하나</b> — ① 확정은 {@code held_qty} 를 {@code sold_qty} 로 옮기고,
 * 웹훅 재시도(재확정)는 재고를 두 번 옮기지 않는다(멱등). ② 결제 확정과 HOLD 만료가
 * 동시에 같은 예약을 건드려도 <b>정확히 하나만</b> 성공하고, 재고는 정확히 한 번만
 * 이동한다 — 예약 행 락(D-025)이 직렬화 지점이기 때문이다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservationConfirmTest {

	private static final LocalDate NIGHT = LocalDate.of(2030, 5, 1);

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationConfirmService confirmService;
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
				.tenantId(1L).code("CFM").name("확정 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("CFMBAR").name("기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		// 재고 1개짜리 하룻밤. 확정과 만료가 이 1을 두고 다툰다.
		roomInventoryRepository.save(RoomInventory.builder()
				.tenantId(1L).roomTypeId(roomTypeId).stayDate(NIGHT)
				.totalQty(1).soldQty(0).heldQty(0)
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
	@DisplayName("확정 → held→sold 이동, 재확정은 멱등(이중 이동 없음)")
	void confirm_movesHeldToSold_andIsIdempotent() {
		Long id = createHold("idem-confirm");
		assertThat(inventory().getHeldQty()).isEqualTo(1);
		assertThat(inventory().getSoldQty()).isZero();

		confirmService.confirm(id);
		assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(inventory().getHeldQty()).as("held 반환").isZero();
		assertThat(inventory().getSoldQty()).as("sold 증가").isEqualTo(1);

		// 웹훅 재시도 — 다시 확정해도 재고는 그대로.
		confirmService.confirm(id);
		assertThat(inventory().getHeldQty()).isZero();
		assertThat(inventory().getSoldQty()).as("이중 이동 없음").isEqualTo(1);
	}

	@RepeatedTest(8)
	@DisplayName("확정 vs 만료 동시 실행 → 정확히 하나만 성공, 재고 정합")
	void confirmVsExpire_exactlyOneWins() throws InterruptedException {
		Long id = createHold("idem-race");
		LocalDateTime future = LocalDateTime.now().plusHours(1); // 만료가 이 시점엔 due

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);
		AtomicBoolean confirmOk = new AtomicBoolean(false);
		AtomicReference<Throwable> confirmErr = new AtomicReference<>();
		AtomicInteger expiredCount = new AtomicInteger();

		pool.submit(() -> {
			try {
				start.await();
				confirmService.confirm(id);
				confirmOk.set(true);
			} catch (Throwable t) {
				confirmErr.set(t); // 만료가 먼저면 확정은 IllegalStateException 으로 거부됨
			} finally {
				done.countDown();
			}
		});
		pool.submit(() -> {
			try {
				start.await();
				expiredCount.set(holdExpiryScheduler.sweep(future));
			} catch (Throwable ignored) {
			} finally {
				done.countDown();
			}
		});

		start.countDown();
		done.await(30, TimeUnit.SECONDS);
		pool.shutdownNow();

		Reservation r = reservationRepository.findById(id).orElseThrow();
		RoomInventory inv = inventory();

		// 재고는 어느 경우든 held=0, total 유지. 절대 오버부킹/불일치 없음.
		assertThat(inv.getHeldQty()).as("held").isZero();
		assertThat(inv.getTotalQty()).isEqualTo(1);

		if (r.getStatus() == ReservationStatus.CONFIRMED) {
			// 확정 승리: 확정 성공 신호, sold=1, 만료는 0건 처리
			assertThat(confirmOk.get()).isTrue();
			assertThat(inv.getSoldQty()).isEqualTo(1);
			assertThat(expiredCount.get()).isZero();
		} else {
			// 만료 승리: 상태 EXPIRED, sold=0, 확정은 거부됨, 만료 1건 처리
			assertThat(r.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
			assertThat(inv.getSoldQty()).isZero();
			assertThat(confirmOk.get()).as("만료가 이겼으면 확정은 실패해야").isFalse();
			assertThat(confirmErr.get()).isNotNull();
			assertThat(expiredCount.get()).isEqualTo(1);
		}
	}
}
