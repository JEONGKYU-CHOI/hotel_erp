package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * 예약 조회.
 *
 * <p><b>여기에는 재고 락 메서드가 없다.</b> 예약 확정의 락 대상은 {@code reservation} 이
 * 아니라 {@code room_inventory} 다(D-018). 예약 행은 매번 새로 INSERT 되므로 경합 상대가
 * 없고, 더블클릭·재시도로 인한 중복은 {@code idempotencyKey} 유니크 제약이 막는다.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/**
	 * 멱등키로 기존 예약을 찾는다.
	 *
	 * <p>같은 요청이 두 번 들어오면(네트워크 재시도·더블클릭) 새로 만들지 않고 이미 만든
	 * 예약을 그대로 돌려주기 위한 조회다. 유니크 제약이 최후에 막지만, 그 예외를 잡아
	 * 처리하는 것보다 먼저 조회해 같은 결과를 주는 편이 사용자 경험이 낫다.
	 */
	Optional<Reservation> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

	/** 비회원 예약 조회 (예약번호 + 전화번호). 인증 없이 도는 경로다. */
	Optional<Reservation> findByTenantIdAndReservationNoAndGuestPhone(
			Long tenantId, String reservationNo, String guestPhone);

	boolean existsByReservationNo(String reservationNo);

	/**
	 * 예약 행을 {@code FOR UPDATE} 로 잠근 채 조회한다.
	 *
	 * <p>상태 전이(확정·만료·취소)가 같은 예약을 두고 경합할 때, 이 행 락이
	 * <b>직렬화 지점</b>이 된다. 먼저 잠근 트랜잭션이 전이를 끝내고 커밋하면, 뒤이은
	 * 트랜잭션은 <b>최신</b> 상태(락 읽기는 스냅샷이 아닌 최신 커밋을 본다)를 읽고
	 * 자신의 가드/멱등 처리로 넘어간다. 재고 여유와 무관하게 이 예약의 전이가 겹치지 않는다.
	 *
	 * <p>락 순서는 언제나 <b>예약 → 재고</b>다. 확정·만료 서비스 모두 이 순서를 지켜
	 * 데드락을 피한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Reservation r where r.id = :id")
	Optional<Reservation> findByIdForUpdate(Long id);

	/**
	 * 만료된 HOLD 의 id 목록. 스케줄러가 훑는 경로다({@code idx_reservation_hold} 사용).
	 *
	 * <p>엔티티가 아니라 <b>id 만</b> 가져온다. 실제 만료 처리는 건마다 별도 트랜잭션에서
	 * id 로 다시 로드해 재고 락을 잡고 진행한다 — 한 번에 수백 건을 한 트랜잭션에 잠그면
	 * 락 보유가 길어지고 그 사이 예약이 대기한다. 배치 크기는 {@link Limit} 으로 제한한다.
	 */
	@Query("""
			select r.id from Reservation r
			where r.tenantId = :tenantId
			  and r.status = :status
			  and r.holdExpiresAt < :now
			order by r.holdExpiresAt
			""")
	List<Long> findDueHoldIds(Long tenantId, ReservationStatus status,
			LocalDateTime now, Limit limit);
}
