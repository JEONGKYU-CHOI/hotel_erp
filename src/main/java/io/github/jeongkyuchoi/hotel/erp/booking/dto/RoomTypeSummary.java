package io.github.jeongkyuchoi.hotel.erp.booking.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import java.util.List;
import java.util.stream.Stream;

/**
 * 부킹엔진 노출용 객실타입 요약(D-030). 고객이 어떤 타입을 고를지 보여주는 최소 정보다.
 *
 * <p>가격·재고는 담지 않는다 — 그건 가용 조회({@code /api/availability})와 요금 계산의 몫이다.
 * 여기서는 "무슨 타입이 팔리는가"만 준다.
 *
 * @param id                타입 식별자. 가용 조회·HOLD 요청에 싣는다
 * @param code              타입 코드
 * @param name              고객 노출 이름
 * @param nameEn            영문 노출 이름(없으면 null → 프론트가 name 으로 폴백, D-051)
 * @param standardOccupancy 기준 인원
 * @param maxOccupancy      최대 인원
 * @param imageUrls         등록된 이미지 URL(최대 3장, 빈 값 제외). 없으면 빈 리스트 → 프론트가 폴백
 */
public record RoomTypeSummary(
		Long id,
		String code,
		String name,
		String nameEn,
		int standardOccupancy,
		int maxOccupancy,
		List<String> imageUrls) {

	public static RoomTypeSummary from(RoomType t) {
		List<String> images = Stream.of(t.getImageUrl(), t.getImageUrl2(), t.getImageUrl3())
				.filter(s -> s != null && !s.isBlank())
				.toList();
		return new RoomTypeSummary(
				t.getId(), t.getCode(), t.getName(), t.getNameEn(),
				t.getStandardOccupancy(), t.getMaxOccupancy(), images);
	}
}
