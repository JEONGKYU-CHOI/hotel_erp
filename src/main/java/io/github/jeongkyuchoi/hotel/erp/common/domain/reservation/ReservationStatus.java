package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

/**
 * 예약 상태.
 *
 * <p>별도의 Hold 테이블을 두지 않고 {@link #HOLD} 상태가 임시점유를 겸한다(D-003).
 * 예약번호를 미리 발급해 PG 에 넘길 수 있고, 결제 성공 시 행을 옮기지 않고
 * 상태만 전이하면 된다.
 *
 * <p>정상 흐름
 * <pre>
 *   HOLD ──결제성공──> CONFIRMED ──체크인──> CHECKED_IN ──체크아웃──> CHECKED_OUT
 *     │                    │                    │
 *     │결제실패/시간초과     │취소                 │
 *     ↓                    ↓                    │
 *  EXPIRED             CANCELLED                │
 *                                               │
 *   CONFIRMED ──당일 미투숙(야간마감)──> NO_SHOW ──┘
 * </pre>
 */
public enum ReservationStatus {

	/** 임시점유. {@code hold_expires_at} 까지만 유효하며 재고의 held_qty 를 잡고 있다. */
	HOLD,
	/** 결제 완료. 재고의 sold_qty 를 차지한다. */
	CONFIRMED,
	/** 투숙 중. 이 시점에 호실이 배정된다. */
	CHECKED_IN,
	/** 퇴실 완료 */
	CHECKED_OUT,
	/** 고객 또는 직원에 의한 취소 */
	CANCELLED,
	/** 예약했으나 오지 않음. 야간마감이 판정한다. */
	NO_SHOW,
	/** HOLD 가 만료됨. 스케줄러가 전이시키며 held_qty 를 되돌린다. */
	EXPIRED;

	/**
	 * 이 상태의 예약이 재고를 점유하고 있는가.
	 *
	 * <p>가용 재고 계산과 재고 복원 판단의 기준이다. 취소·만료·노쇼는 점유하지 않는다.
	 */
	public boolean occupiesInventory() {
		return this == HOLD || this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
	}
}
