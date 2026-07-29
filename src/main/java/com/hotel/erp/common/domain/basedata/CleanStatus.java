package com.hotel.erp.common.domain.basedata;

/**
 * 호실 청결 상태 (하우스키핑).
 *
 * <p>하우스키핑 모듈 자체는 1차 범위 밖이지만, 체크아웃 시 {@link #DIRTY} 로 바꾸는
 * 한 줄만으로 설계 서사가 성립하므로 상태값은 미리 갖춰 둔다.
 */
public enum CleanStatus {
	/** 청소 완료 */
	CLEAN,
	/** 청소 필요 */
	DIRTY,
	/** 청소 후 점검 완료 */
	INSPECTED,
	/** 고장 등으로 판매 불가 */
	OUT_OF_ORDER
}
