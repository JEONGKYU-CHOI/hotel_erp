package io.github.jeongkyuchoi.hotel.erp.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Gender;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Member;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 판매 지표 집계 테스트(추천 메일 1단계). demo 성격의 예약 세트를 직접 심어 집계 결과를 고정한다.
 *
 * <p><b>무엇을 증명하나</b>
 * <ul>
 *   <li>기준날짜는 체크아웃일이고, 대상 달력 월 밖(전달·다음달)의 예약은 세지 않는다.</li>
 *   <li>상태를 세 갈래로 나눈다 — CHECKED_OUT=체크아웃(랭킹 기준), CONFIRMED·CHECKED_IN=예약,
 *       NO_SHOW=노쇼. HOLD·EXPIRED·CANCELLED 는 어디에도 세지 않는다.</li>
 *   <li>세그먼트는 성별×나이대(출생연도→기준연도 기준 만나이 근사)로 나뉘고, 비회원은
 *       {@link Segment#GUEST} 전체집계로 모인다.</li>
 *   <li>결과는 체크아웃 건수 내림차순(추천 랭킹 순)으로 정렬된다.</li>
 * </ul>
 *
 * <p>다른 시더·테스트와 섞이지 않도록 전용 테넌트(777)로 격리한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SalesAggregationServiceTest {

	private static final Long TENANT = 777L;
	private static final YearMonth MONTH = YearMonth.of(2033, 5);
	private static final LocalDate IN_MONTH = LocalDate.of(2033, 5, 10);
	private static final LocalDate PREV_MONTH = LocalDate.of(2033, 4, 28);
	private static final LocalDate NEXT_MONTH = LocalDate.of(2033, 6, 2);

	@Autowired private SalesAggregationService service;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private MemberRepository memberRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private final AtomicInteger seq = new AtomicInteger();

	private RoomType std;
	private RoomType dlx;
	private RatePlan plan;
	private Member male20s;   // 2005년생 → 2033년 기준 28세
	private Member female30s; // 1998년생 → 2033년 기준 35세

	@BeforeEach
	void seed() {
		std = roomTypeRepository.save(RoomType.builder()
				.tenantId(TENANT).code("AGG-STD").name("집계 스탠다드")
				.standardOccupancy(2).maxOccupancy(3).bedType("트윈").displayOrder(1).active(true)
				.build());
		dlx = roomTypeRepository.save(RoomType.builder()
				.tenantId(TENANT).code("AGG-DLX").name("집계 디럭스")
				.standardOccupancy(2).maxOccupancy(3).bedType("더블").displayOrder(2).active(true)
				.build());
		plan = ratePlanRepository.save(RatePlan.builder()
				.tenantId(TENANT).roomType(std).code("AGG-P1").name("집계 기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0.10")).active(true)
				.build());

		male20s = saveMember("male20s@test.io", Gender.MALE, LocalDate.of(2005, 6, 1));
		female30s = saveMember("female30s@test.io", Gender.FEMALE, LocalDate.of(1998, 3, 20));
	}

	@AfterEach
	void cleanup() {
		// 예약이 회원·요금제·객실타입을 FK 로 참조하므로 예약부터 지운다.
		reservationRepository.deleteAll();
		memberRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	@Test
	@DisplayName("체크아웃일 기준 달력 월을 세그먼트×타입×요금제로 집계 — 상태 3분류·비회원 전체집계·랭킹 정렬")
	void aggregatesMonthlySales() {
		// (MALE,20대) STD/P1 — 체크아웃2, 예약(확정1+투숙중1=2), 노쇼1
		reserve(male20s, std, ReservationStatus.CHECKED_OUT, IN_MONTH);
		reserve(male20s, std, ReservationStatus.CHECKED_OUT, IN_MONTH);
		reserve(male20s, std, ReservationStatus.CONFIRMED, IN_MONTH);
		reserve(male20s, std, ReservationStatus.CHECKED_IN, IN_MONTH);
		reserve(male20s, std, ReservationStatus.NO_SHOW, IN_MONTH);
		// (MALE,20대) DLX/P1 — 체크아웃1
		reserve(male20s, dlx, ReservationStatus.CHECKED_OUT, IN_MONTH);

		// (FEMALE,30대) STD/P1 — 체크아웃1
		reserve(female30s, std, ReservationStatus.CHECKED_OUT, IN_MONTH);

		// 비회원 STD/P1 — 체크아웃3 (전체집계 세그먼트)
		reserve(null, std, ReservationStatus.CHECKED_OUT, IN_MONTH);
		reserve(null, std, ReservationStatus.CHECKED_OUT, IN_MONTH);
		reserve(null, std, ReservationStatus.CHECKED_OUT, IN_MONTH);

		// 세지 않는 상태 — 어디에도 안 잡혀야 한다.
		reserve(male20s, std, ReservationStatus.HOLD, IN_MONTH);
		reserve(male20s, std, ReservationStatus.CANCELLED, IN_MONTH);
		reserve(male20s, std, ReservationStatus.EXPIRED, IN_MONTH);

		// 대상 월 밖 — 체크아웃일이 전달/다음달이면 제외.
		reserve(male20s, std, ReservationStatus.CHECKED_OUT, PREV_MONTH);
		reserve(male20s, std, ReservationStatus.CHECKED_OUT, NEXT_MONTH);

		List<SalesMetrics> result = service.aggregateMonth(TENANT, MONTH);

		// 버킷은 정확히 4개: (남20대 STD), (남20대 DLX), (여30대 STD), (비회원 STD)
		assertThat(result).hasSize(4);

		SalesMetrics maleStd = pick(result, Gender.MALE, AgeBand.TWENTIES, std.getId());
		assertThat(maleStd.checkoutCount()).isEqualTo(2);
		assertThat(maleStd.reservedCount()).isEqualTo(2);
		assertThat(maleStd.noShowCount()).isEqualTo(1);

		SalesMetrics maleDlx = pick(result, Gender.MALE, AgeBand.TWENTIES, dlx.getId());
		assertThat(maleDlx.checkoutCount()).isEqualTo(1);
		assertThat(maleDlx.reservedCount()).isZero();
		assertThat(maleDlx.noShowCount()).isZero();

		SalesMetrics femaleStd = pick(result, Gender.FEMALE, AgeBand.THIRTIES, std.getId());
		assertThat(femaleStd.checkoutCount()).isEqualTo(1);

		SalesMetrics guestStd = result.stream()
				.filter(m -> m.segment().isGuest() && m.roomTypeId() == std.getId())
				.findFirst().orElseThrow();
		assertThat(guestStd.segment()).isEqualTo(Segment.GUEST);
		assertThat(guestStd.checkoutCount()).isEqualTo(3);

		// 랭킹 정렬: 체크아웃 내림차순 → 비회원(3), 남20대 STD(2)가 앞선다.
		assertThat(result.get(0)).isEqualTo(guestStd);
		assertThat(result.get(1)).isEqualTo(maleStd);
		assertThat(result).extracting(SalesMetrics::checkoutCount)
				.containsExactly(3L, 2L, 1L, 1L);
	}

	@Test
	@DisplayName("대상 월에 판매가 없으면 빈 리스트")
	void emptyWhenNoSales() {
		reserve(male20s, std, ReservationStatus.CHECKED_OUT, NEXT_MONTH);
		assertThat(service.aggregateMonth(TENANT, MONTH)).isEmpty();
	}

	private SalesMetrics pick(List<SalesMetrics> result, Gender gender, AgeBand band, Long roomTypeId) {
		return result.stream()
				.filter(m -> band.equals(m.segment().ageBand())
						&& gender.equals(m.segment().gender())
						&& m.roomTypeId() == roomTypeId)
				.findFirst().orElseThrow(() ->
						new AssertionError("세그먼트 없음: " + gender + "/" + band + " type=" + roomTypeId));
	}

	private Member saveMember(String email, Gender gender, LocalDate birthDate) {
		return memberRepository.save(Member.builder()
				.tenantId(TENANT).email(email).passwordHash("x").name(email).phone("010-0000-0000")
				.gender(gender).birthDate(birthDate).marketingConsent(true)
				.status(MemberStatus.ACTIVE)
				.build());
	}

	private void reserve(Member member, RoomType roomType, ReservationStatus status, LocalDate checkOut) {
		int n = seq.incrementAndGet();
		reservationRepository.save(Reservation.builder()
				.tenantId(TENANT).reservationNo("AGG" + n)
				.member(member)
				.guestName(member == null ? "비회원" : member.getName())
				.guestPhone("010-0000-0000")
				.roomType(roomType).ratePlan(plan)
				.checkInDate(checkOut.minusDays(1)).checkOutDate(checkOut)
				.adults(2).children(0)
				.status(status).totalAmount(new BigDecimal("100000"))
				.idempotencyKey("idem-agg-" + n)
				.build());
	}
}
