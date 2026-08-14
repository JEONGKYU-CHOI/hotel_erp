package io.github.jeongkyuchoi.hotel.erp.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.analytics.AgeBand;
import io.github.jeongkyuchoi.hotel.erp.analytics.Segment;
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
 * 회원별 추천 후보 선정 테스트(추천 메일 2단계). demo 성격의 예약 세트를 심어 병합 규칙을 고정한다.
 *
 * <p><b>무엇을 증명하나</b>
 * <ul>
 *   <li>본인 이력(최근 순)을 먼저, 세그먼트 인기(랭킹 순)로 남는 자리를 채운다.</li>
 *   <li>같은 객실타입·요금제는 중복으로 담지 않고, 후보는 최대 3개다.</li>
 *   <li>이력이 없는 회원도 세그먼트 인기만으로 후보를 받는다.</li>
 *   <li>인기 근거는 발송 월 직전 3개월(체크아웃일 기준) — 그 밖의 투숙은 랭킹에 안 든다.</li>
 *   <li>마케팅 미동의·프로필 미상 회원은 발송 대상에서 빠진다(단, 인기 집계에는 기여한다).</li>
 * </ul>
 *
 * <p>전용 테넌트(888)로 격리한다. 발송 월 2033-08 → 인기 윈도우는 2033-05·06·07.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RecommendationCandidateServiceTest {

	private static final Long TENANT = 888L;
	private static final YearMonth SEND_MONTH = YearMonth.of(2033, 8);

	@Autowired private RecommendationCandidateService service;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private MemberRepository memberRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private final AtomicInteger seq = new AtomicInteger();

	private RoomType std;   // A
	private RoomType dlx;   // B
	private RoomType suite; // C
	private RoomType pent;  // D — 윈도우 밖 투숙에만 쓴다
	private RatePlan plan;

	@BeforeEach
	void seed() {
		std = roomType("REC-STD", "추천 스탠다드", 1);
		dlx = roomType("REC-DLX", "추천 디럭스", 2);
		suite = roomType("REC-SUITE", "추천 스위트", 3);
		pent = roomType("REC-PENT", "추천 펜트", 4);
		plan = ratePlanRepository.save(RatePlan.builder()
				.tenantId(TENANT).roomType(std).code("REC-P").name("추천 기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0.10")).active(true)
				.build());
	}

	@AfterEach
	void cleanup() {
		reservationRepository.deleteAll();
		memberRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	@Test
	@DisplayName("본인 이력 먼저·인기로 채움·중복제거·최대 3개, 그리고 이력 없는 회원은 인기만")
	void selectsCandidatesPerMember() {
		// 발송 대상: 남 20대(이력 있음), 여 30대(이력 없음). 둘 다 수신동의·ACTIVE.
		Member male20s = member("male@test.io", Gender.MALE, LocalDate.of(2005, 6, 1), true);
		Member female30s = member("female@test.io", Gender.FEMALE, LocalDate.of(1998, 3, 20), true);
		// 인기만 만들고 발송 대상은 아닌 미동의 회원들(집계에는 기여).
		Member male20sOther = member("male2@test.io", Gender.MALE, LocalDate.of(2005, 1, 1), false);
		Member female30sOther = member("female2@test.io", Gender.FEMALE, LocalDate.of(1998, 5, 5), false);
		// 프로필 미상(성별 null) — 수신동의여도 세그먼트를 못 잡아 제외.
		memberRepository.save(Member.builder()
				.tenantId(TENANT).email("noprofile@test.io").passwordHash("x").name("미상")
				.phone("010-0000-0000").gender(null).birthDate(null).marketingConsent(true)
				.status(MemberStatus.ACTIVE).build());

		// male20s 본인 이력: STD(최근 7월) → SUITE(오래된 5월). 둘 다 윈도우 안이라 인기에도 1건씩.
		checkout(male20s, std, LocalDate.of(2033, 7, 15));
		checkout(male20s, suite, LocalDate.of(2033, 5, 10));
		// male20sOther: DLX ×3 (남 20대 인기 1위 만들기).
		checkout(male20sOther, dlx, LocalDate.of(2033, 6, 1));
		checkout(male20sOther, dlx, LocalDate.of(2033, 6, 5));
		checkout(male20sOther, dlx, LocalDate.of(2033, 6, 10));
		// male20sOther: PENT ×2 이지만 윈도우 밖(4월) → 인기에 잡히면 안 된다.
		checkout(male20sOther, pent, LocalDate.of(2033, 4, 20));
		checkout(male20sOther, pent, LocalDate.of(2033, 4, 25));
		// female30sOther: STD ×2 (여 30대 인기).
		checkout(female30sOther, std, LocalDate.of(2033, 6, 15));
		checkout(female30sOther, std, LocalDate.of(2033, 6, 20));

		List<MemberRecommendation> result = service.selectFor(TENANT, SEND_MONTH);

		// 발송 대상은 수신동의·프로필 있는 두 명뿐.
		assertThat(result).hasSize(2);

		MemberRecommendation male = byMember(result, male20s.getId());
		assertThat(male.segment()).isEqualTo(new Segment(Gender.MALE, AgeBand.TWENTIES));
		// 이력 STD·SUITE 먼저(최근 순), 인기 1위 DLX 로 3번째 채움. PENT(윈도우 밖)는 없다.
		assertThat(male.items()).extracting(RecommendationItem::roomTypeId)
				.containsExactly(std.getId(), suite.getId(), dlx.getId());
		assertThat(male.items()).extracting(RecommendationItem::source)
				.containsExactly(RecommendationSource.HISTORY, RecommendationSource.HISTORY,
						RecommendationSource.POPULAR);
		assertThat(male.items()).extracting(RecommendationItem::roomTypeId)
				.doesNotContain(pent.getId());

		MemberRecommendation female = byMember(result, female30s.getId());
		assertThat(female.segment()).isEqualTo(new Segment(Gender.FEMALE, AgeBand.THIRTIES));
		// 이력 없음 → 인기만. 여 30대 인기는 STD 하나.
		assertThat(female.items()).hasSize(1);
		assertThat(female.items().get(0).roomTypeId()).isEqualTo(std.getId());
		assertThat(female.items().get(0).source()).isEqualTo(RecommendationSource.POPULAR);
	}

	@Test
	@DisplayName("이력도 인기도 없는 회원은 발송 대상에서 빠진다")
	void memberWithNoCandidatesExcluded() {
		Member lonely = member("lonely@test.io", Gender.MALE, LocalDate.of(2005, 6, 1), true);
		// 이 회원의 세그먼트에 인기 데이터가 전혀 없고, 본인 이력도 없다.
		assertThat(service.selectFor(TENANT, SEND_MONTH)).isEmpty();
		// (lonely 는 참조만 하고 예약을 만들지 않는다.)
		assertThat(lonely.getId()).isNotNull();
	}

	private MemberRecommendation byMember(List<MemberRecommendation> result, Long memberId) {
		return result.stream().filter(r -> r.memberId().equals(memberId)).findFirst()
				.orElseThrow(() -> new AssertionError("추천 없음: memberId=" + memberId));
	}

	private RoomType roomType(String code, String name, int order) {
		return roomTypeRepository.save(RoomType.builder()
				.tenantId(TENANT).code(code).name(name)
				.standardOccupancy(2).maxOccupancy(3).bedType("트윈").displayOrder(order).active(true)
				.build());
	}

	private Member member(String email, Gender gender, LocalDate birthDate, boolean consent) {
		return memberRepository.save(Member.builder()
				.tenantId(TENANT).email(email).passwordHash("x").name(email).phone("010-0000-0000")
				.gender(gender).birthDate(birthDate).marketingConsent(consent)
				.unsubscribeToken("tok-" + seq.incrementAndGet())
				.status(MemberStatus.ACTIVE).build());
	}

	private void checkout(Member member, RoomType roomType, LocalDate checkOut) {
		int n = seq.incrementAndGet();
		reservationRepository.save(Reservation.builder()
				.tenantId(TENANT).reservationNo("REC" + n)
				.member(member).guestName(member.getName()).guestPhone("010-0000-0000")
				.roomType(roomType).ratePlan(plan)
				.checkInDate(checkOut.minusDays(1)).checkOutDate(checkOut)
				.adults(2).children(0)
				.status(ReservationStatus.CHECKED_OUT).totalAmount(new BigDecimal("100000"))
				.idempotencyKey("idem-rec-" + n)
				.build());
	}
}
