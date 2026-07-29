package com.hotel.erp.common.domain.basedata;

/**
 * 호실 점유 상태.
 *
 * <p>점유(occupancy)와 청결(clean)을 2축으로 분리한다. 한 컬럼에 몰면
 * VACANT_CLEAN / VACANT_DIRTY / OCCUPIED_CLEAN … 으로 조합이 폭발한다.
 * 상세는 {@code V1__init_schema.sql} 의 {@code room} 블록 주석 참조.
 */
public enum OccupancyStatus {
	/** 비어 있음 */
	VACANT,
	/** 투숙 중 */
	OCCUPIED
}
