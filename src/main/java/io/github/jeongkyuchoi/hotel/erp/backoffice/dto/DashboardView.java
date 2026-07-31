package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
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
		List<ReservationListRow> recent,
		List<Room> housekeeping) {
}
