package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 예약 숙박분 조회. 야간마감이 "그날 밤 게시 대상"을 찾는 경로다(D-026).
 */
public interface ReservationNightRepository extends JpaRepository<ReservationNight, Long> {

	/**
	 * 특정 영업일의 <b>미게시 숙박분</b>을 게시 대상 예약에 한해 조회한다.
	 *
	 * <p>게시 대상은 실제로 그날 묵는 예약, 즉 {@code CONFIRMED} 또는 {@code CHECKED_IN}
	 * 상태다. HOLD·EXPIRED·CANCELLED·NO_SHOW 는 숙박료를 게시하지 않는다.
	 *
	 * <p>{@code posted = false} 필터가 숙박분 단위 멱등성의 핵심이다 — 두 번째 마감이
	 * 돌아도 이미 게시된 숙박분은 이 조회에서 빠져 재게시되지 않는다(V2 주석 ②).
	 *
	 * <p>{@code idx_reservation_night_date (stay_date, posted)} 를 탄다.
	 */
	@Query("""
			select n from ReservationNight n
			join n.reservation r
			where n.stayDate = :businessDate
			  and n.posted = false
			  and r.tenantId = :tenantId
			  and r.status in :postableStatuses
			order by n.id
			""")
	List<ReservationNight> findPostableNights(
			@Param("tenantId") Long tenantId,
			@Param("businessDate") LocalDate businessDate,
			@Param("postableStatuses") List<ReservationStatus> postableStatuses);

	/**
	 * 특정 영업일의 <b>객실 실적</b>을 집계한다 — 그날 밤 실제로 묵는 숙박분의 요금 합(매출)과
	 * 판매된 객실 수(숙박분 건수). 대시보드의 매출·ADR·가동률 분자로 쓴다.
	 *
	 * <p>대상은 {@code findPostableNights} 와 같은 규율 — 그날 묵는 예약({@code CONFIRMED}·
	 * {@code CHECKED_IN})만. HOLD·취소·노쇼는 매출이 아니다. {@code posted} 여부와 무관하게
	 * "그날 밤 점유"를 세므로 야간마감 전/후 어느 시점에 조회해도 같은 값을 준다.
	 *
	 * <p>집계라 항상 한 행을 돌려준다. 그날 묵는 예약이 없으면 {@code sum} 은 null 이고
	 * {@link RoomRevenueStat} 생성자가 0 으로 보정한다.
	 */
	@Query("""
			select new io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.RoomRevenueStat(
			    sum(n.rateAmount), count(n))
			from ReservationNight n
			join n.reservation r
			where n.stayDate = :date
			  and r.tenantId = :tenantId
			  and r.status in :occupiedStatuses
			""")
	RoomRevenueStat aggregateRoomRevenue(
			@Param("tenantId") Long tenantId,
			@Param("date") LocalDate date,
			@Param("occupiedStatuses") List<ReservationStatus> occupiedStatuses);
}
