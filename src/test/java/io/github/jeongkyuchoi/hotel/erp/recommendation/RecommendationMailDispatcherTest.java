package io.github.jeongkyuchoi.hotel.erp.recommendation;

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
import io.github.jeongkyuchoi.hotel.erp.notification.EmailMessage;
import io.github.jeongkyuchoi.hotel.erp.notification.EmailSender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * 추천 메일 발송 배치 테스트(추천 메일 4단계). 발송 메시지를 가로채는 테스트용 {@link EmailSender}
 * 로 본문을 확인하고, 발송 이력으로 멱등을 검증한다.
 *
 * <p><b>무엇을 증명하나</b>
 * <ul>
 *   <li>후보 회원 전원에게 한 통씩 발송하고 이력을 남긴다.</li>
 *   <li>메일 본문에 추천 객실명과 회원 토큰이 박힌 수신거부 링크가 들어간다.</li>
 *   <li>같은 달 재실행 시 이미 보낸 회원은 건너뛴다(멱등) — 메일도 이력도 늘지 않는다.</li>
 * </ul>
 *
 * <p>전용 테넌트(999)로 격리. 발송 월 2033-08 → 인기·이력 윈도우는 2033-05·06·07.
 */
@Import({TestcontainersConfiguration.class, RecommendationMailDispatcherTest.RecordingMailConfig.class})
@SpringBootTest
class RecommendationMailDispatcherTest {

	private static final Long TENANT = 999L;
	private static final YearMonth SEND_MONTH = YearMonth.of(2033, 8);

	@Autowired private RecommendationMailDispatcher dispatcher;
	@Autowired private RecommendationMailLogRepository logRepository;
	@Autowired private RecordingEmailSender recorder;
	@Autowired private ReservationRepository reservationRepository;
	@Autowired private MemberRepository memberRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;
	@Autowired private RatePlanRepository ratePlanRepository;

	private final AtomicInteger seq = new AtomicInteger();

	private RoomType std;
	private RoomType dlx;
	private RatePlan plan;

	@BeforeEach
	void seed() {
		recorder.clear();
		std = roomType("MAIL-STD", "메일 스탠다드");
		dlx = roomType("MAIL-DLX", "메일 디럭스");
		plan = ratePlanRepository.save(RatePlan.builder()
				.tenantId(TENANT).roomType(std).code("MAIL-P").name("메일 기본요금")
				.baseAmount(new BigDecimal("100000")).breakfastIncluded(false).refundable(true)
				.cancelDeadlineDays((short) 1).penaltyRate(new BigDecimal("0.10")).active(true)
				.build());
	}

	@AfterEach
	void cleanup() {
		logRepository.deleteAll();
		reservationRepository.deleteAll();
		memberRepository.deleteAll();
		ratePlanRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	@Test
	@DisplayName("후보 전원 발송·본문·수신거부 링크, 그리고 재실행 멱등")
	void dispatchesAndIsIdempotent() {
		// 수신동의 회원 둘, 각자 본인 이력으로 추천거리 확보.
		Member male = member("male@test.io", Gender.MALE, LocalDate.of(2005, 6, 1), "tok-male");
		Member female = member("female@test.io", Gender.FEMALE, LocalDate.of(1998, 3, 20), "tok-female");
		checkout(male, std, LocalDate.of(2033, 6, 10));
		checkout(female, dlx, LocalDate.of(2033, 6, 12));

		DispatchResult first = dispatcher.dispatch(TENANT, SEND_MONTH);

		assertThat(first).isEqualTo(new DispatchResult(2, 2, 0, 0));
		assertThat(logRepository.count()).isEqualTo(2);
		assertThat(recorder.sent).hasSize(2);

		// 남성 회원 메일: 추천 객실명 + 토큰 박힌 수신거부 링크.
		EmailMessage maleMail = recorder.byRecipient("male@test.io");
		assertThat(maleMail.subject()).contains("추천");
		assertThat(maleMail.body())
				.contains(std.getName())
				.contains("/api/marketing/unsubscribe?token=tok-male");

		// 재실행 — 이미 보낸 둘은 건너뛴다. 메일도 이력도 늘지 않는다.
		recorder.clear();
		DispatchResult second = dispatcher.dispatch(TENANT, SEND_MONTH);

		assertThat(second).isEqualTo(new DispatchResult(2, 0, 2, 0));
		assertThat(recorder.sent).isEmpty();
		assertThat(logRepository.count()).isEqualTo(2);
	}

	private RoomType roomType(String code, String name) {
		return roomTypeRepository.save(RoomType.builder()
				.tenantId(TENANT).code(code).name(name)
				.standardOccupancy(2).maxOccupancy(3).bedType("트윈").displayOrder(1).active(true)
				.build());
	}

	private Member member(String email, Gender gender, LocalDate birthDate, String token) {
		return memberRepository.save(Member.builder()
				.tenantId(TENANT).email(email).passwordHash("x").name(email).phone("010-0000-0000")
				.gender(gender).birthDate(birthDate).marketingConsent(true)
				.unsubscribeToken(token).status(MemberStatus.ACTIVE).build());
	}

	private void checkout(Member member, RoomType roomType, LocalDate checkOut) {
		int n = seq.incrementAndGet();
		reservationRepository.save(Reservation.builder()
				.tenantId(TENANT).reservationNo("MAIL" + n)
				.member(member).guestName(member.getName()).guestPhone("010-0000-0000")
				.roomType(roomType).ratePlan(plan)
				.checkInDate(checkOut.minusDays(1)).checkOutDate(checkOut)
				.adults(2).children(0)
				.status(ReservationStatus.CHECKED_OUT).totalAmount(new BigDecimal("100000"))
				.idempotencyKey("idem-mail-" + n)
				.build());
	}

	/** 발송 메일을 메모리에 모으는 테스트용 전송기. 실제 로그 전송기 대신 @Primary 로 끼운다. */
	static class RecordingEmailSender implements EmailSender {
		final List<EmailMessage> sent = new ArrayList<>();

		@Override
		public void send(EmailMessage message) {
			sent.add(message);
		}

		void clear() {
			sent.clear();
		}

		EmailMessage byRecipient(String to) {
			return sent.stream().filter(m -> m.to().equals(to)).findFirst()
					.orElseThrow(() -> new AssertionError("수신자 메일 없음: " + to));
		}
	}

	@TestConfiguration
	static class RecordingMailConfig {
		@Bean
		@Primary
		RecordingEmailSender recordingEmailSender() {
			return new RecordingEmailSender();
		}
	}
}
