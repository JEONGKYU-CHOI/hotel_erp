package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 HOLD 를 주기적으로 청소한다(D-003).
 *
 * <p><b>왜 스케줄러가 루프를 돌고, 만료 로직은 {@link ReservationExpiryService} 에 있나</b><br>
 * 예약 한 건의 만료는 독립 트랜잭션이어야 한다(한 건 실패가 배치를 무너뜨리지 않게, 락도
 * 짧게). Spring 의 {@code @Transactional} 은 같은 빈 안에서 자기 메서드를 부르면 프록시를
 * 거치지 않아 트랜잭션이 걸리지 않는다. 그래서 루프(트랜잭션 없음)는 이 컴포넌트에 두고,
 * 건별 처리({@code expireOne}, 트랜잭션)는 다른 빈에 두어 프록시를 통해 부른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryScheduler {

	/** 한 번에 처리할 최대 건수. 폭주 상황에서 한 주기가 너무 오래 물리지 않게 막는다. */
	private static final int BATCH_LIMIT = 200;

	private final ReservationExpiryService expiryService;

	/**
	 * 만료 HOLD 청소. {@code fixedDelay} 라 이전 실행이 끝난 뒤 다음 실행이 시작된다 —
	 * 청소가 밀려도 겹쳐 돌지 않는다. {@code initialDelay} 로 기동 직후 폭주를 피한다.
	 */
	@Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
	public void scheduled() {
		int cleaned = sweep(LocalDateTime.now());
		if (cleaned > 0) {
			log.info("HOLD 만료 청소: {}건 처리", cleaned);
		}
	}

	/**
	 * 주어진 시각 기준으로 만료분을 청소하고 처리 건수를 돌려준다.
	 *
	 * <p>스케줄러와 테스트가 함께 쓴다. 시각을 파라미터로 받아 테스트가 결정적으로
	 * 검증할 수 있게 한다.
	 */
	public int sweep(LocalDateTime now) {
		List<Long> dueIds = expiryService.findDueHoldIds(now, BATCH_LIMIT);
		int processed = 0;
		for (Long id : dueIds) {
			try {
				expiryService.expireOne(id, now);
				processed++;
			} catch (RuntimeException e) {
				// 한 건 실패가 나머지를 막지 않게 삼키고 로그만 남긴다. 다음 주기에 재시도된다.
				log.warn("HOLD 만료 처리 실패 id={} — 다음 주기에 재시도", id, e);
			}
		}
		return processed;
	}
}
