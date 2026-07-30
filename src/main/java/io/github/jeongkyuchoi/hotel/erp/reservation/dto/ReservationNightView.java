package io.github.jeongkyuchoi.hotel.erp.reservation.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 예약 상세의 일자별 숙박 요금 한 줄(D-028).
 *
 * @param stayDate   숙박 일자
 * @param rateAmount 그 날의 요금 스냅샷
 * @param posted     야간마감이 게시했는지 (D-026)
 */
public record ReservationNightView(LocalDate stayDate, BigDecimal rateAmount, boolean posted) {

	static ReservationNightView from(ReservationNight night) {
		return new ReservationNightView(night.getStayDate(), night.getRateAmount(), night.isPosted());
	}
}
