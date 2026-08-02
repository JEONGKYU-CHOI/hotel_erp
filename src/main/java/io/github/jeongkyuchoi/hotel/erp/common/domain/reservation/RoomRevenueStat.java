package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import java.math.BigDecimal;

/**
 * 특정 영업일의 객실 실적 집계 — 숙박분 요금 합과 판매 객실 수.
 * 대시보드 매출·ADR·가동률의 분자/분모로 쓴다.
 *
 * @param revenue 숙박분 요금 합. 그날 묵는 예약이 없으면 {@code sum} 이 null 이라 생성 시 0 으로 보정된다.
 * @param rooms   판매된 객실 수(숙박분 건수).
 */
public record RoomRevenueStat(BigDecimal revenue, long rooms) {

	public RoomRevenueStat(BigDecimal revenue, long rooms) {
		this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
		this.rooms = rooms;
	}
}
