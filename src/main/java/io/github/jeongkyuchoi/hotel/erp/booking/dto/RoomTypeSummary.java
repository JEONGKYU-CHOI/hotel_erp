package io.github.jeongkyuchoi.hotel.erp.booking.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;

/**
 * 부킹엔진 노출용 객실타입 요약(D-030). 고객이 어떤 타입을 고를지 보여주는 최소 정보다.
 *
 * <p>가격·재고는 담지 않는다 — 그건 가용 조회({@code /api/availability})와 요금 계산의 몫이다.
 * 여기서는 "무슨 타입이 팔리는가"만 준다.
 *
 * @param id                타입 식별자. 가용 조회·HOLD 요청에 싣는다
 * @param code              타입 코드
 * @param name              고객 노출 이름
 * @param standardOccupancy 기준 인원
 * @param maxOccupancy      최대 인원
 */
public record RoomTypeSummary(
		Long id,
		String code,
		String name,
		int standardOccupancy,
		int maxOccupancy) {

	public static RoomTypeSummary from(RoomType t) {
		return new RoomTypeSummary(
				t.getId(), t.getCode(), t.getName(),
				t.getStandardOccupancy(), t.getMaxOccupancy());
	}
}
