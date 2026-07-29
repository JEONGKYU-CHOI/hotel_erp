package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

/**
 * 요금 캘린더 생성 결과.
 *
 * <p>재고 생성({@link InventoryGenerateResult})과 달리 "건너뜀 사유"가 없다.
 * 요금은 오버부킹처럼 막아야 할 제약이 없어, 다른 금액이면 그냥 갱신한다.
 * 같은 금액이라 손대지 않은 날짜만 {@code unchanged} 로 센다.
 *
 * @param created   새로 만든 일자 수
 * @param updated   기존 행의 금액을 바꾼 일자 수
 * @param unchanged 이미 같은 금액이라 건드리지 않은 일자 수
 */
public record RateCalendarGenerateResult(int created, int updated, int unchanged) {

	public int total() {
		return created + updated + unchanged;
	}
}
