package com.hotel.erp.common.domain.inventory;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 재고 조회/락 리포지토리.
 *
 * <p>연박 예약은 여러 날짜 행을 한 트랜잭션에서 잠근다. 락 획득 순서가 요청마다
 * 다르면 데드락이 나므로 항상 {@code stay_date} 오름차순으로 잠근다.
 * 그 순서를 실제로 보장하는 것은 {@code ORDER BY} 가 아니라
 * {@code (room_type_id, stay_date)} 를 선두로 갖는 인덱스다(D-015).
 */
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

	/**
	 * 락 없는 일반 조회. 스파이크에서 "락이 걸린 조회"와 대조군으로 쓴다.
	 */
	List<RoomInventory> findByRoomTypeIdAndStayDateBetweenOrderByStayDate(
			Long roomTypeId, LocalDate from, LocalDate to);

	/**
	 * JPQL + {@code @Lock(PESSIMISTIC_WRITE)}.
	 *
	 * <p>Hibernate 가 방언(dialect)에 맞춰 잠금 구문을 붙인다. MySQL 이면
	 * {@code for update} 다. 실제로 붙어 나가는지는 로그로 확인해야 한다
	 * — 이것이 이 스파이크의 존재 이유다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select i from RoomInventory i
			where i.roomTypeId = :roomTypeId
			  and i.stayDate between :from and :to
			order by i.stayDate
			""")
	List<RoomInventory> lockForUpdate(
			@Param("roomTypeId") Long roomTypeId,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/**
	 * native query 로 {@code FOR UPDATE} 를 직접 쓴 버전(D-002).
	 *
	 * <p>SQL 이 코드에 그대로 드러나 무엇을 어떤 순서로 잠그는지 읽힌다.
	 * 위 JPQL 버전과 실제 나가는 SQL 이 같은지 스파이크에서 대조한다.
	 */
	@Query(value = """
			SELECT * FROM room_inventory
			WHERE room_type_id = :roomTypeId
			  AND stay_date BETWEEN :from AND :to
			ORDER BY stay_date
			FOR UPDATE
			""", nativeQuery = true)
	List<RoomInventory> lockForUpdateNative(
			@Param("roomTypeId") Long roomTypeId,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/**
	 * 백오피스 재고 현황 조회 (락 없음, 읽기 전용).
	 *
	 * <p><b>이 메서드를 예약 확정 트랜잭션에서 부르면 안 된다.</b> 한 번이라도 부르면
	 * 그 행이 영속성 컨텍스트에 등록되고, 뒤이은 락 조회가 DB 의 최신값 대신 캐시의
	 * 옛 값을 돌려준다(D-018). 화면 표시 전용이다.
	 */
	List<RoomInventory> findByTenantIdAndRoomTypeIdAndStayDateBetweenOrderByStayDate(
			Long tenantId, Long roomTypeId, LocalDate from, LocalDate to);
}
