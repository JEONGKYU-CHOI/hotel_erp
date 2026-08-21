package io.github.jeongkyuchoi.hotel.erp.recommendation;

import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 추천 메일을 매월 한 번 발송한다(추천 메일 4단계).
 *
 * <p>발송 로직은 {@link RecommendationMailDispatcher} 가 갖고, 이 컴포넌트는 크론으로 그것을
 * 부르기만 한다({@code NightCloseScheduler} 와 같은 트리거/로직 분리). 발송은 회원 × 발송월당
 * 멱등하므로 크론이 겹쳐 기동하거나 관리자가 수동 트리거를 함께 눌러도 이중 발송되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationMailScheduler {

	/** 단일 테넌트 데모(백오피스 서비스들과 같은 상수). 멀티테넌트가 되면 대상 테넌트를 순회한다. */
	private static final Long DEMO_TENANT = 1L;

	private final RecommendationMailDispatcher dispatcher;

	/**
	 * 매월 1일 09:00 에 이번 달 추천 메일을 발송한다. 인기 근거는 직전 3개 완료월이다(2단계 규칙).
	 */
	@Scheduled(cron = "0 0 9 1 * *")
	public void scheduled() {
		DispatchResult result = dispatcher.dispatch(DEMO_TENANT, YearMonth.now());
		log.info("월간 추천 메일 스케줄 실행 → {}", result);
	}
}
