package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** 호실 조회. */
public interface RoomRepository extends JpaRepository<Room, Long> {

	/**
	 * 호실 행을 {@code FOR UPDATE} 로 잠근 채 조회한다. 체크인 배정의 직렬화 지점이다(D-031).
	 *
	 * <p>두 예약이 같은 호실을 동시에 배정하려 하면, 먼저 잠근 쪽이 {@code occupy()} 로
	 * OCCUPIED 로 바꾸고 커밋한다. 뒤이은 쪽은 락 읽기로 최신 OCCUPIED 를 보고
	 * {@code occupy()} 가드에서 거부된다. 락 순서는 언제나 <b>예약 → 호실</b>이다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.id = :id")
	Optional<Room> findByIdForUpdate(Long id);

	/**
	 * 특정 객실타입의 배정 가능한 호실. 체크인 화면의 호실 선택지다(D-031).
	 *
	 * <p>배정 조건({@link Room#isAssignable})을 쿼리로 옮겼다 — 활성·공실·청소완료/점검완료.
	 * 정렬은 호실번호 사전순이다.
	 */
	@Query("""
			select r from Room r
			where r.tenantId = :tenantId
			  and r.roomType.id = :roomTypeId
			  and r.active = true
			  and r.occupancyStatus = io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.OccupancyStatus.VACANT
			  and r.cleanStatus in (
			      io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.CleanStatus.CLEAN,
			      io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.CleanStatus.INSPECTED)
			order by r.roomNo asc
			""")
	List<Room> findAssignable(Long tenantId, Long roomTypeId);

	/**
	 * 백오피스 목록.
	 *
	 * <p>{@code join fetch} 로 객실타입을 한 번에 가져온다. 이게 없으면 호실 100건을
	 * 조회할 때 타입 이름을 찍는 순간마다 SELECT 가 한 번씩 더 나간다(N+1 문제).
	 * 연관을 LAZY 로 둔 대가를 이렇게 갚는다.
	 *
	 * <p>정렬은 층 → 호실번호 순인데, 호실번호가 문자열이라 사전순이다.
	 * ('101', '102', '1010' 순). 1차 범위에서는 문제가 되지 않는다.
	 */
	@Query("""
			select r from Room r
			join fetch r.roomType
			where r.tenantId = :tenantId
			order by r.floor asc nulls last, r.roomNo asc
			""")
	List<Room> findAllForList(Long tenantId);

	Optional<Room> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByTenantIdAndRoomNo(Long tenantId, String roomNo);

	boolean existsByTenantIdAndRoomNoAndIdNot(Long tenantId, String roomNo, Long id);

	/** 객실타입을 판매 중단하기 전에 딸린 호실이 있는지 확인하는 용도. */
	long countByRoomTypeIdAndActiveTrue(Long roomTypeId);
}
