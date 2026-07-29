package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 일자별 요금 조회.
 *
 * <p><b>왜 락 메서드가 없는가 (D-022)</b> — {@code room_inventory} 와 달리
 * {@code rate_calendar} 는 예약 트랜잭션이 동시에 잠그는 행이 아니다. 예약은 재고의
 * {@code sold_qty}/{@code held_qty} 를 올릴 뿐, 요금표를 수정하지 않는다(예약 시점
 * 금액은 {@code reservation_night} 스냅샷으로 복사된다). 요금표를 바꾸는 것은 관리자
 * 한 명이고, 그 동시성은 유니크 제약 {@code uk_rate_calendar} 만으로 충분하다.
 * 그래서 재고 생성이 {@code lockForUpdateNative} 로 시작하는 것과 달리, 여기 생성은
 * 일반 조회로 시작한다.
 */
public interface RateCalendarRepository extends JpaRepository<RateCalendar, Long> {

	/** 화면 표시·생성 병합용 범위 조회. */
	List<RateCalendar> findByTenantIdAndRatePlanIdAndStayDateBetweenOrderByStayDate(
			Long tenantId, Long ratePlanId, LocalDate from, LocalDate to);

	/** 목록 화면. 요금정책·객실타입을 함께 가져와 N+1 을 피한다. */
	@Query("""
			select rc from RateCalendar rc
			join fetch rc.ratePlan rp
			join fetch rp.roomType rt
			where rc.tenantId = :tenantId
			  and rc.ratePlan.id = :ratePlanId
			  and rc.stayDate between :from and :to
			order by rc.stayDate
			""")
	List<RateCalendar> findRangeForList(Long tenantId, Long ratePlanId,
			LocalDate from, LocalDate to);
}
