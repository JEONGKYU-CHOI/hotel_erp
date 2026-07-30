package io.github.jeongkyuchoi.hotel.erp.reservation.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.NightClose;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 야간마감 실행 결과(D-026).
 *
 * <p>{@code alreadyClosed} 가 이 결과의 핵심이다 — 같은 영업일을 다시 마감하면
 * {@code alreadyClosed = true} 에 게시 건수 0 으로 돌아온다. 재실행이 아무 것도
 * 게시하지 않았음을 호출자·테스트가 이 플래그로 확인한다.
 *
 * @param businessDate     마감 영업일
 * @param postedNightCount 이번 실행이 실제로 게시한 숙박분 수 (재마감이면 0)
 * @param postedAmount     이번 실행이 게시한 요금 합계 (재마감이면 0)
 * @param alreadyClosed    이미 마감돼 있어 이번 실행이 게시하지 않았으면 true
 */
public record NightCloseResult(
		LocalDate businessDate,
		int postedNightCount,
		BigDecimal postedAmount,
		boolean alreadyClosed) {

	/** 이번 실행이 실제로 마감을 수행했다. */
	public static NightCloseResult closed(NightClose closed) {
		return new NightCloseResult(closed.getBusinessDate(), closed.getPostedNightCount(),
				closed.getPostedAmount(), false);
	}

	/** 이미 마감된 영업일이라 아무 것도 게시하지 않았다(선조회 멱등). */
	public static NightCloseResult alreadyClosed(NightClose existing) {
		return new NightCloseResult(existing.getBusinessDate(), 0, BigDecimal.ZERO, true);
	}
}
