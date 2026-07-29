package com.hotel.erp.backoffice.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 재고 생성 결과.
 *
 * <p>"몇 건 처리했다"만 돌려주지 않는다. 건너뛴 날짜와 <b>그 사유</b>를 함께 돌려주는
 * 것이 핵심이다. 이미 예약이 잡힌 날짜는 판매 총량을 줄일 수 없어 조용히 건너뛰게
 * 되는데, 그것을 알려주지 않으면 담당자는 "생성했는데 왜 그날만 수량이 다르지?"를
 * 나중에 예약 사고로 발견한다.
 *
 * @param created 새로 만든 일자 수
 * @param updated 기존 행의 판매 총량을 갱신한 일자 수
 * @param skipped 건너뛴 일자와 사유
 */
public record InventoryGenerateResult(
		int created,
		int updated,
		List<Skipped> skipped) {

	public record Skipped(LocalDate stayDate, String reason) {}

	public int total() {
		return created + updated + skipped.size();
	}

	public boolean hasSkipped() {
		return !skipped.isEmpty();
	}
}
