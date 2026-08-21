package io.github.jeongkyuchoi.hotel.erp.recommendation;

import java.time.YearMonth;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 추천 메일 월간 발송 배치의 루프(추천 메일 4단계). 후보 회원을 뽑아 한 명씩
 * {@link RecommendationMailService#sendOne} 으로 넘긴다.
 *
 * <p>루프 자신은 트랜잭션이 없다 — 건별 트랜잭션은 분리된 {@link RecommendationMailService}
 * 가 갖는다(HOLD 만료 배치와 같은 구조, {@code HoldExpiryScheduler} 참조). 한 건 실패는
 * 삼켜 로그만 남기고 다음 회원으로 넘어간다. 실패분은 이력이 없으므로 다음 실행에서 재시도된다.
 *
 * <p>스케줄러 자동 실행과 관리자 수동 트리거가 함께 이 진입점을 쓴다. 발송 월을 파라미터로
 * 받아 테스트·수동 실행이 대상 달을 정할 수 있게 한다.
 */
@Slf4j
@Component
public class RecommendationMailDispatcher {

	private final RecommendationCandidateService candidateService;
	private final RecommendationMailService mailService;

	public RecommendationMailDispatcher(RecommendationCandidateService candidateService,
			RecommendationMailService mailService) {
		this.candidateService = candidateService;
		this.mailService = mailService;
	}

	/**
	 * 주어진 발송 월 기준으로 후보 회원 전원에게 추천 메일을 발송한다. 이미 보낸 회원은
	 * 건너뛴다(멱등). 대상 달 직전 3개월을 인기 근거로 본다(2단계 규칙).
	 */
	public DispatchResult dispatch(Long tenantId, YearMonth sendMonth) {
		List<MemberRecommendation> candidates = candidateService.selectFor(tenantId, sendMonth);
		int sent = 0;
		int skipped = 0;
		int failed = 0;
		for (MemberRecommendation rec : candidates) {
			try {
				if (mailService.sendOne(tenantId, sendMonth, rec)) {
					sent++;
				} else {
					skipped++;
				}
			} catch (RuntimeException e) {
				failed++;
				log.warn("추천 메일 발송 실패 memberId={} — 다음 실행에서 재시도", rec.memberId(), e);
			}
		}
		DispatchResult result = new DispatchResult(candidates.size(), sent, skipped, failed);
		log.info("추천 메일 배치 완료 tenant={} month={} → {}", tenantId, sendMonth, result);
		return result;
	}
}
