package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.DashboardView;
import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.ReservationListRow;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 백오피스 대시보드 집계. 로그인 직후 /admin 에 오늘의 운영 현황을 모아 보여준다.
 *
 * <p>예약을 한 번에 읽어(목록 쿼리 재사용, {@code join fetch roomType}) 오늘 도착·출발·재실·
 * 신규를 자바에서 센다 — 이 앱 규모의 예약 수라면 단순·명료가 낫다. 하우스키핑 현황은 현황판
 * 쿼리를 그대로 쓴다. 모두 읽기 전용 트랜잭션 안에서 조립한다(OSIV 꺼짐).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

	private static final Long TENANT_ID = 1L;
	private static final int RECENT_LIMIT = 6;

	private final ReservationRepository reservationRepository;
	private final RoomRepository roomRepository;

	@Transactional(readOnly = true)
	public DashboardView load() {
		LocalDate today = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();

		List<Reservation> all = reservationRepository.findForAdminList(TENANT_ID, null, null, null);

		long arrivals = all.stream()
				.filter(r -> r.getStatus() == ReservationStatus.CONFIRMED
						&& today.equals(r.getCheckInDate()))
				.count();
		long departures = all.stream()
				.filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN
						&& today.equals(r.getCheckOutDate()))
				.count();
		long inHouse = all.stream()
				.filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN)
				.count();
		long newToday = all.stream()
				.filter(r -> r.getCreatedAt() != null
						&& today.equals(r.getCreatedAt().toLocalDate()))
				.count();
		long active = all.stream()
				.filter(r -> r.getStatus() == ReservationStatus.HOLD
						|| r.getStatus() == ReservationStatus.CONFIRMED
						|| r.getStatus() == ReservationStatus.CHECKED_IN)
				.count();

		List<ReservationListRow> recent = all.stream()
				.sorted(Comparator.comparing(Reservation::getId).reversed())
				.limit(RECENT_LIMIT)
				.map(r -> ReservationListRow.from(r, now))
				.toList();

		List<Room> housekeeping = roomRepository.findForHousekeeping(TENANT_ID);

		return new DashboardView(arrivals, departures, inHouse, newToday, active,
				housekeeping.size(), recent, housekeeping);
	}
}
