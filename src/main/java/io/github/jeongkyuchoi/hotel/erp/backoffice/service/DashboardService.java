package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.DashboardView;
import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.ReservationListRow;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNightRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.RoomRevenueStat;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

	/** 그날 밤 실제로 묵는(매출·점유로 잡는) 예약 상태 — 야간마감 게시 대상과 같은 규율(D-026). */
	private static final List<ReservationStatus> OCCUPIED_STATUSES =
			List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

	private final ReservationRepository reservationRepository;
	private final RoomRepository roomRepository;
	private final ReservationNightRepository reservationNightRepository;
	private final RoomInventoryRepository roomInventoryRepository;

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

		// 오늘의 실적 KPI — 매출·판매객실은 숙박분에서, 총객실은 재고에서. 세 지표가
		// 같은 판매객실 수를 공유해 서로 어긋나지 않는다(OCC 분자 = ADR 분모).
		RoomRevenueStat stat = reservationNightRepository
				.aggregateRoomRevenue(TENANT_ID, today, OCCUPIED_STATUSES);
		BigDecimal roomRevenue = stat.revenue();
		long soldRooms = stat.rooms();
		long totalRooms = roomInventoryRepository.sumTotalQtyOn(TENANT_ID, today);

		double occupancyPct = totalRooms == 0
				? 0.0
				: BigDecimal.valueOf(soldRooms * 100.0 / totalRooms)
						.setScale(1, RoundingMode.HALF_UP).doubleValue();
		BigDecimal adr = soldRooms == 0
				? BigDecimal.ZERO
				: roomRevenue.divide(BigDecimal.valueOf(soldRooms), 0, RoundingMode.HALF_UP);

		return new DashboardView(arrivals, departures, inHouse, newToday, active,
				housekeeping.size(), roomRevenue, occupancyPct, adr, soldRooms, totalRooms,
				recent, housekeeping);
	}
}
