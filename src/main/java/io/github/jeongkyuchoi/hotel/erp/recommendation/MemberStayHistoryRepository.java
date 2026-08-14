package io.github.jeongkyuchoi.hotel.erp.recommendation;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * 회원의 과거 투숙 이력 조회(추천 메일 2단계). 예약을 읽기만 하므로 쓰기 없는
 * {@link Repository} 마커만 상속한다.
 */
public interface MemberStayHistoryRepository extends Repository<Reservation, Long> {

	/**
	 * 회원이 과거 <b>체크아웃</b>한 객실타입·요금제를 최근 투숙 순으로 돌려준다.
	 *
	 * <p><b>CHECKED_OUT 만 세는 이유</b> — "실제로 묵고 만족했을 상품"만 재방문 유도의 근거가
	 * 된다. 예약만 하고 취소·노쇼한 건은 본인 이력으로 밀어 줄 이유가 없다. 같은 조합을 여러 번
	 * 묵었으면 {@code group by} 로 한 줄로 접고, {@code max(checkOutDate)} 로 최신 투숙일을 잡아
	 * 최근에 묵은 상품이 위로 오게 정렬한다 — 추천 후보 병합에서 본인 이력을 먼저 채우기 때문이다.
	 */
	@Query("""
			select new io.github.jeongkyuchoi.hotel.erp.recommendation.MemberStayRow(
				rt.id, rt.name, rp.id, rp.name, max(r.checkOutDate))
			from Reservation r
			join r.roomType rt
			join r.ratePlan rp
			where r.tenantId = :tenantId
			  and r.member.id = :memberId
			  and r.status = io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus.CHECKED_OUT
			group by rt.id, rt.name, rp.id, rp.name
			order by max(r.checkOutDate) desc
			""")
	List<MemberStayRow> findStayedRoomTypes(Long tenantId, Long memberId);
}
