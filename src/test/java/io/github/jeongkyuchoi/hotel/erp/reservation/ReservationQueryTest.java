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
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationDetail;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationQueryService;
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
 * 예약 조회 테스트 — 비회원 경로(예약번호 + 전화, D-028).
 *
 * <p><b>무엇을 증명하나</b> — ① 예약번호+전화가 맞으면 상세를 조립해 돌려준다(연관·야간
 * 포함, 트랜잭션 밖 접근에도 안전). ② 미존재·전화 불일치는 같은 예외로 구분되지 않는다.
 * ③ 만료 시각이 지난 HOLD 는 스케줄러 정리 전이라도 EXPIRED 로 표시된다(D-003).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservationQueryTest {

	private static final LocalDate D0 = LocalDate.of(2032, 9, 10);
	private static final LocalDate CHECK_OUT = D0.plusDays(2); // 2박
	private static final String PHONE = "010-1234-5678";

	@Autowired private ReservationService reservationService;
	@Autowired private ReservationQueryService queryService;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private RoomInventoryRepository roomInventoryRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private Long roomTypeId;
	private Long ratePlanId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("QRY").name("조회 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
		ratePlanId = ratePlanRepository.save(RatePlan.builder()
				.tenantId(1L).roomType(roomType).code("QRYBAR").name("조식포함")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(true).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0")).active(true)
				.build()).getId();
		for (LocalDate d : new LocalDate[] {D0, D0.plusDays(1)}) {
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

	private Reservation createHold() {
		return reservationService.hold(new ReservationHoldCommand(
				null, "홍길동", PHONE, "gil@example.com",
				roomTypeId, ratePlanId, D0, CHECK_OUT, 2, 1, "idem-qry"));
	}

	@Test
	@DisplayName("예약번호+전화 일치 → 상세 조립(연관·야간 포함)")
	void findForGuest_returnsDetail() {
		String no = createHold().getReservationNo();

		ReservationDetail d = queryService.findForGuest(no, PHONE);

		assertThat(d.reservationNo()).isEqualTo(no);
		assertThat(d.status()).isEqualTo(ReservationStatus.HOLD);
		assertThat(d.guestName()).isEqualTo("홍길동");
		assertThat(d.roomTypeCode()).isEqualTo("QRY");
		assertThat(d.ratePlanName()).isEqualTo("조식포함");
		assertThat(d.nights()).isEqualTo(2);
		assertThat(d.adults()).isEqualTo(2);
		assertThat(d.children()).isEqualTo(1);
		assertThat(d.totalAmount()).isEqualByComparingTo("200000");
		assertThat(d.nightViews()).hasSize(2);
		assertThat(d.nightViews().get(0).stayDate()).isEqualTo(D0);
	}

	@Test
	@DisplayName("미존재 예약번호 → NotFound")
	void findForGuest_unknownNo_throws() {
		createHold();
		assertThatThrownBy(() -> queryService.findForGuest("R000000XXXX", PHONE))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	@DisplayName("전화 불일치 → 미존재와 같은 NotFound (존재여부 비노출)")
	void findForGuest_wrongPhone_throwsSameAsUnknown() {
		String no = createHold().getReservationNo();
		assertThatThrownBy(() -> queryService.findForGuest(no, "010-0000-0000"))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	@DisplayName("만료 시각 지난 HOLD 는 스케줄러 정리 전이라도 EXPIRED 로 표시(D-003)")
	void expiredHold_shownExpired() {
		// 상태는 HOLD 지만 holdExpiresAt 이 과거인 예약. 스케줄러가 아직 정리하지 못한
		// 상태를 재현한다. 표시 규칙은 조립 로직(from)이 now 기준으로 판정하므로 여기서
		// 통제된 now 로 직접 검증한다 — DB 를 만지지 않는다.
		RoomType roomType = roomTypeRepository.findById(roomTypeId).orElseThrow();
		RatePlan ratePlan = ratePlanRepository.findById(ratePlanId).orElseThrow();
		LocalDateTime expiredAt = LocalDateTime.of(2032, 9, 1, 0, 0);

		Reservation held = Reservation.builder()
				.tenantId(1L).reservationNo("R320901ABCD")
				.guestName("홍길동").guestPhone(PHONE)
				.roomType(roomType).ratePlan(ratePlan)
				.checkInDate(D0).checkOutDate(D0.plusDays(1)).adults(2).children(0)
				.status(ReservationStatus.HOLD).totalAmount(new BigDecimal("100000"))
				.holdExpiresAt(expiredAt).idempotencyKey("idem-exp-view")
				.build();

		// now 가 만료 시각 이후 → EXPIRED 로 표시.
		ReservationDetail after = ReservationDetail.from(held, expiredAt.plusMinutes(1));
		assertThat(after.status()).as("만료 후 조회 → EXPIRED").isEqualTo(ReservationStatus.EXPIRED);

		// now 가 만료 전 → 여전히 HOLD.
		ReservationDetail before = ReservationDetail.from(held, expiredAt.minusMinutes(1));
		assertThat(before.status()).as("만료 전 조회 → HOLD").isEqualTo(ReservationStatus.HOLD);
	}
}
