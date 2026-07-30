package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.reservation.dto.NightCloseResult;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 야간마감을 하루 한 번 실행한다(D-004, D-026).
 *
 * <p><b>왜 스케줄러(루프/트리거)와 마감 로직({@link NightCloseService})을 나누나</b> —
 * {@link HoldExpiryScheduler} 와 같은 이유다. 마감 로직은 트랜잭션 경계 안에서 돌아야 하고
 * ({@code @Transactional}), Spring 프록시는 자기 빈의 메서드 호출에 걸리지 않는다. 트리거는
 * 트랜잭션이 없는 이 컴포넌트에, 트랜잭션 로직은 다른 빈에 둔다.
 *
 * <p><b>멱등하므로 크론이 겹쳐 기동해도 안전하다.</b> 배치 재시도·중복 실행에서도 같은
 * 영업일이 두 번 게시되지 않는다({@link NightCloseService} 참조). 그래서 배치 인프라 없이
 * 단순 {@code @Scheduled} 로 충분하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NightCloseScheduler {

	private final NightCloseService nightCloseService;

	/**
	 * 매일 03:05 에 <b>직전 영업일</b>(방금 끝난 밤)을 마감한다.
	 *
	 * <p>자정을 넘긴 뒤 도는 관행적 야간감사(night audit)라, 마감 대상은 어제 날짜다 —
	 * 오늘 새벽 03:05 에 돌면 어젯밤(어제 stay_date) 숙박분을 게시한다.
	 */
	@Scheduled(cron = "0 5 3 * * *")
	public void scheduled() {
		run(LocalDate.now().minusDays(1));
	}

	/**
	 * 주어진 영업일을 마감하고 결과를 돌려준다.
	 *
	 * <p>스케줄러와 테스트가 함께 쓴다. 영업일을 파라미터로 받아 테스트가 결정적으로
	 * 검증할 수 있게 한다({@link HoldExpiryScheduler#sweep} 과 같은 규약).
	 */
	public NightCloseResult run(LocalDate businessDate) {
		NightCloseResult result = nightCloseService.close(businessDate, "SYSTEM");
		if (result.alreadyClosed()) {
			log.info("야간마감 스킵 — 이미 마감된 영업일 date={}", businessDate);
		} else {
			log.info("야간마감 실행 date={} 게시 {}건 합계={}",
					businessDate, result.postedNightCount(), result.postedAmount());
		}
		return result;
	}
}
