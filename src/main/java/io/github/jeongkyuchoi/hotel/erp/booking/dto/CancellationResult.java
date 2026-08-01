package io.github.jeongkyuchoi.hotel.erp.booking.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.CancellationCharge;
import java.math.BigDecimal;

/**
 * 고객 예약 취소 결과. 화면이 위약금·환불 안내를 그리는 데 쓴다.
 *
 * @param reservationNo 취소된 예약번호
 * @param status        취소 후 상태(항상 CANCELLED)
 * @param penalty       위약금(원). HOLD 취소는 0.
 * @param refund        환불 예정/실행 금액(원). 총액 − 위약금.
 * @param refunded      실제 토스 결제취소가 실행됐는지(결제된 예약을 취소한 경우만 true).
 * @param basis         판정 근거(UNPAID/FREE/DEADLINE_PASSED/NON_REFUNDABLE/SETTLED).
 */
public record CancellationResult(
		String reservationNo,
		String status,
		BigDecimal penalty,
		BigDecimal refund,
		boolean refunded,
		String basis) {

	public static CancellationResult of(String reservationNo, CancellationCharge charge, boolean refunded) {
		return new CancellationResult(reservationNo, "CANCELLED",
				charge.penalty(), charge.refund(), refunded, charge.basis().name());
	}
}
