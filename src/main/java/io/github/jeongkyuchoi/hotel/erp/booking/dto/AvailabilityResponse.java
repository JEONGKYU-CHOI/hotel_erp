package io.github.jeongkyuchoi.hotel.erp.booking.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 가용 재고 조회 응답(D-030). 부킹엔진이 날짜 범위를 고를 때 부른다.
 *
 * <p>{@code bookableQty} 는 <b>전 숙박일의 최소 가용</b>이다 — 연박은 all-or-nothing 이라
 * 어느 하루라도 재고가 없으면 그 범위는 팔 수 없다. 0 이면 예약 버튼을 막는 신호다.
 *
 * @param bookableQty 이 범위로 예약 가능한 객실 수 (모든 밤의 최소 가용)
 * @param nights      일자별 가용 내역
 */
public record AvailabilityResponse(
		Long roomTypeId,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		int nightCount,
		int bookableQty,
		List<NightAvailability> nights) {

	/**
	 * 하루치 가용.
	 *
	 * @param availableQty 총량 − 확정 − 점유. 재고 행이 없으면 0(생성 전이라 판매 불가).
	 */
	public record NightAvailability(LocalDate stayDate, int availableQty) {
	}
}
