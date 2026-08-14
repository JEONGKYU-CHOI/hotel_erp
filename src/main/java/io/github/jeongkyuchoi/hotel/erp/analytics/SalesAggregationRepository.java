package io.github.jeongkyuchoi.hotel.erp.analytics;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * 판매 지표 집계 조회(추천 메일 1단계). 예약을 읽기만 하므로 쓰기 메서드가 없는
 * {@link Repository} 마커만 상속한다 — 이 리포지토리로 예약을 저장·삭제할 일은 없다.
 *
 * <p>집계 규칙(설계 합의, D-054 후속)
 * <ul>
 *   <li><b>기간</b> — 기준날짜는 <b>체크아웃일</b>({@code check_out_date}). 달력 월 하나를
 *       {@code [monthStart, nextMonthStart)} 반열린 구간으로 받는다.</li>
 *   <li><b>집계 단위</b> — 객실타입 × 요금제, 그리고 세그먼트(성별×출생연도).</li>
 *   <li><b>상태</b> — 실제 판매만 센다. {@code HOLD·EXPIRED·CANCELLED} 는 호출자가 넘기는
 *       {@code statuses} 집합에서 빠져 애초에 조회되지 않는다.</li>
 *   <li><b>비회원</b> — 회원을 {@code left join} 해, 비회원은 {@code gender·birthYear} 가
 *       null 로 나온다. 서비스가 이를 전체집계 세그먼트로 접는다.</li>
 * </ul>
 */
public interface SalesAggregationRepository extends Repository<Reservation, Long> {

	/**
	 * 체크아웃일이 {@code [monthStart, nextMonthStart)} 에 드는 예약을
	 * 객실타입 × 요금제 × 성별 × 출생연도 × 상태로 묶어 건수를 센다.
	 *
	 * <p>나이대가 아니라 출생연도로 묶는 이유는 {@link SalesRow} 주석 참조 — 나이대 변환은
	 * 기준월에 의존하는 파생값이라 서비스가 자바에서 처리한다.
	 *
	 * @param statuses 셀 상태 집합. 판매로 인정하는 {@code CHECKED_OUT·CONFIRMED·CHECKED_IN·NO_SHOW}
	 *                 만 넘긴다. 빈 집합을 넘기면 아무것도 조회되지 않는다.
	 */
	@Query("""
			select new io.github.jeongkyuchoi.hotel.erp.analytics.SalesRow(
				rt.id, rt.name, rp.id, rp.name,
				m.gender, year(m.birthDate), r.status, count(r))
			from Reservation r
			join r.roomType rt
			join r.ratePlan rp
			left join r.member m
			where r.tenantId = :tenantId
			  and r.checkOutDate >= :monthStart
			  and r.checkOutDate < :nextMonthStart
			  and r.status in :statuses
			group by rt.id, rt.name, rp.id, rp.name, m.gender, year(m.birthDate), r.status
			""")
	List<SalesRow> aggregate(Long tenantId, LocalDate monthStart, LocalDate nextMonthStart,
			Collection<ReservationStatus> statuses);
}
