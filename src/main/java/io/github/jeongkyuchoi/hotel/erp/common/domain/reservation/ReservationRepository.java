package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
