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
}
