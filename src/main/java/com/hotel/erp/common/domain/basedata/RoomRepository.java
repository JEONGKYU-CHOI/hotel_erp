package com.hotel.erp.common.domain.basedata;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 호실 조회. */
public interface RoomRepository extends JpaRepository<Room, Long> {

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
