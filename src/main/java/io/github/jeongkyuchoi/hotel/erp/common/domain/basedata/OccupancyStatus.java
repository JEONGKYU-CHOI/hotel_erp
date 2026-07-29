package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 호실 점유 상태.
 *
 * <p>점유(occupancy)와 청결(clean)을 2축으로 분리한다. 한 컬럼에 몰면
 * VACANT_CLEAN / VACANT_DIRTY / OCCUPIED_CLEAN … 으로 조합이 폭발한다.
 * 상세는 {@code V1__init_schema.sql} 의 {@code room} 블록 주석 참조.
 *
 * <p>이 값은 예약과 체크인/체크아웃이 바꾼다. 기준정보 화면에서 직접 고르지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum OccupancyStatus {

	/** 비어 있음 */
	VACANT("공실"),
	/** 투숙 중 */
	OCCUPIED("투숙중");

	private final String label;
}
