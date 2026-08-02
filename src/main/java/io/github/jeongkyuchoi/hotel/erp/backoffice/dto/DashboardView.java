package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import java.math.BigDecimal;
import java.util.List;

/**
 * 백오피스 대시보드 요약. 로그인 직후 착지 화면(/admin)이 오늘의 운영 현황을 한눈에 보인다.
 *
 * @param arrivalsToday       오늘 도착 예정(확정·체크인 대기)
 * @param departuresToday     오늘 출발 예정(투숙 중·체크아웃 대기)
 * @param inHouse             현재 재실(투숙 중)
 * @param newToday            오늘 생성된 예약
 * @param activeReservations  살아있는 예약(임시점유·확정·투숙)
 * @param housekeepingPending 하우스키핑 처리 대상(청소필요·청소중·점검대기)
 * @param roomRevenueToday    오늘 객실 매출(그날 밤 묵는 숙박분 요금 합)
 * @param occupancyPct        오늘 가동률 OCC (판매객실/총객실 × 100, 소수 1자리)
 * @param adrToday            오늘 ADR (객실 매출 / 판매객실, 원 단위 반올림)
 * @param soldRoomsToday      오늘 판매된 객실 수(숙박분 건수) — OCC·ADR 의 공통 분자/분모
 * @param totalRoomsToday     오늘 판매 가능한 총 객실 수 — OCC 의 분모
 * @param recent              최근 예약 몇 건
 * @param housekeeping        하우스키핑 현황 스냅샷
 */
public record DashboardView(
		long arrivalsToday,
		long departuresToday,
		long inHouse,
		long newToday,
		long activeReservations,
		long housekeepingPending,
		BigDecimal roomRevenueToday,
		double occupancyPct,
		BigDecimal adrToday,
		long soldRoomsToday,
		long totalRoomsToday,
		List<ReservationListRow> recent,
		List<Room> housekeeping) {
}
