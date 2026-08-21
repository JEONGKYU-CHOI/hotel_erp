package io.github.jeongkyuchoi.hotel.erp.recommendation;

import io.github.jeongkyuchoi.hotel.erp.notification.EmailMessage;
import io.github.jeongkyuchoi.hotel.erp.notification.EmailSender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추천 메일 한 통을 멱등하게 발송하고 이력을 남긴다(추천 메일 4단계).
 *
 * <p><b>왜 건별로 트랜잭션을 나누나</b> — 배치 루프({@link RecommendationMailDispatcher})와
 * 분리된 별도 빈에 두어, 한 회원 처리가 독립 트랜잭션이 되게 한다. 한 건 실패가 배치 전체를
 * 롤백하지 않고, 이미 보낸 이력은 커밋되어 재실행 시 건너뛴다(HOLD 만료 배치와 같은 구조).
 * 같은 빈 안에서 자기 메서드를 부르면 트랜잭션 프록시를 거치지 않으므로 반드시 분리해야 한다.
 */
@Service
public class RecommendationMailService {

	private final RecommendationMailComposer composer;
	private final EmailSender emailSender;
	private final RecommendationMailLogRepository logRepository;

	public RecommendationMailService(RecommendationMailComposer composer, EmailSender emailSender,
			RecommendationMailLogRepository logRepository) {
		this.composer = composer;
		this.emailSender = emailSender;
		this.logRepository = logRepository;
	}

	/**
	 * 회원 한 명에게 추천 메일을 보낸다. 이미 이 달에 보냈으면(멱등) 아무것도 하지 않고
	 * {@code false} 를 돌려준다. 실제로 보냈으면 이력을 남기고 {@code true} 를 돌려준다.
	 *
	 * @return 이번에 발송했으면 true, 이미 보내 건너뛰었으면 false
	 */
	@Transactional
	public boolean sendOne(Long tenantId, YearMonth sendMonth, MemberRecommendation rec) {
		LocalDate month = sendMonth.atDay(1);
		if (logRepository.existsByTenantIdAndMemberIdAndSendMonth(tenantId, rec.memberId(), month)) {
			return false; // 이미 보냈다 — 멱등 건너뛰기.
		}

		EmailMessage message = composer.compose(rec);
		emailSender.send(message);

		logRepository.save(RecommendationMailLog.builder()
				.tenantId(tenantId)
				.memberId(rec.memberId())
				.sendMonth(month)
				.email(rec.email())
				.subject(message.subject())
				.itemCount(rec.items().size())
				.sentAt(LocalDateTime.now())
				.build());
		return true;
	}
}
