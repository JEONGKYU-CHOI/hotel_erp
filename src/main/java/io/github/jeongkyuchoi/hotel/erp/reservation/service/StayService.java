package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투숙 라이프사이클 — 체크인·체크아웃(D-031).
 *
 * <p>예약 라이프사이클의 뒷단이다: CONFIRMED → <b>CHECKED_IN</b>(호실 배정) →
 * <b>CHECKED_OUT</b>(점유 해제·청소 표시).
 *
 * <p><b>락 순서는 예약 → 호실.</b> 확정·취소가 예약 → 재고를 지키는 것과 같은 규율이다
 * (D-025). 체크인은 예약 행을 먼저 잠가 전이를 직렬화하고, 그다음 호실 행을 잠가 동시 배정을
 * 막는다. 재고는 건드리지 않는다 — 재고는 확정(sold) 시점에 이미 확정됐고, 체크인은 물리
 * 호실을 붙일 뿐이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StayService {

	private static final Long TENANT_ID = 1L;

	private final ReservationRepository reservationRepository;
	private final RoomRepository roomRepository;

	/**
	 * 체크인 — 확정 예약에 호실을 배정하고 CHECKED_IN 으로 전이한다.
	 *
	 * @throws NotFoundException     예약·호실이 없으면.
	 * @throws IllegalArgumentException 예약 객실타입과 다른 호실이면.
	 * @throws IllegalStateException 확정 상태가 아니거나 배정 불가 호실이면.
	 */
	@Transactional
	public Reservation checkIn(Long reservationId, Long roomId) {
		// ★ 예약 행을 먼저 잠근다(예약 → 호실 순서, D-025).
		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new NotFoundException(
						"예약을 찾을 수 없습니다. id=" + reservationId));

		// ★ 호실 행을 잠근다. 동시 배정을 직렬화하는 지점이다(D-031).
		Room room = roomRepository.findByIdForUpdate(roomId)
				.orElseThrow(() -> new NotFoundException("호실을 찾을 수 없습니다. id=" + roomId));

		if (!room.getTenantId().equals(TENANT_ID)
				|| !room.getRoomType().getId().equals(reservation.getRoomType().getId())) {
			throw new IllegalArgumentException(
					"예약 객실타입과 다른 호실입니다. room=" + room.getRoomNo());
		}
		if (!room.isAssignable()) {
			throw new IllegalStateException(
					"배정할 수 없는 호실입니다. room=" + room.getRoomNo()
							+ " 점유=" + room.getOccupancyStatus() + " 청결=" + room.getCleanStatus());
		}

		room.occupy();          // 공실 → 투숙중 (가드: VACANT 아니면 예외)
		reservation.checkIn(room); // CONFIRMED → CHECKED_IN + 호실 배정
		log.info("체크인: no={} room={}", reservation.getReservationNo(), room.getRoomNo());
		return reservation;
	}

	/**
	 * 체크아웃 — 투숙 예약을 CHECKED_OUT 으로 전이하고 호실을 청소 대상으로 되돌린다.
	 *
	 * @throws NotFoundException     예약이 없으면.
	 * @throws IllegalStateException 투숙 중이 아니면.
	 */
	@Transactional
	public Reservation checkOut(Long reservationId) {
		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new NotFoundException(
						"예약을 찾을 수 없습니다. id=" + reservationId));

		Room assigned = reservation.getRoom();
		if (assigned != null) {
			// 배정 호실 행을 잠그고 점유 해제·청소 표시. (예약 → 호실 순서 유지)
			Room room = roomRepository.findByIdForUpdate(assigned.getId()).orElse(assigned);
			room.checkOutVacate();
		}

		reservation.checkOut(); // CHECKED_IN → CHECKED_OUT (가드: 투숙 중 아니면 예외)
		log.info("체크아웃: no={}", reservation.getReservationNo());
		return reservation;
	}
}
