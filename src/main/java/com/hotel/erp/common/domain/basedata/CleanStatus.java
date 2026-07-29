package com.hotel.erp.common.domain.basedata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 호실 청결 상태 (하우스키핑).
 *
 * <p>하우스키핑 모듈 자체는 1차 범위 밖이지만, 체크아웃 시 {@link #DIRTY} 로 바꾸는
 * 한 줄만으로 설계 서사가 성립하므로 상태값은 미리 갖춰 둔다.
 *
 * <p>화면 표시명을 enum 이 직접 들고 있다. 템플릿에서 {@code th:switch} 로 분기하면
 * 상태를 추가할 때 화면 쪽 수정을 빠뜨리게 되고, 그러면 값이 영문 그대로 노출된다.
 */
@Getter
@RequiredArgsConstructor
public enum CleanStatus {

	/** 청소 완료 */
	CLEAN("청소완료"),
	/** 청소 필요 */
	DIRTY("청소필요"),
	/** 청소 후 점검 완료 */
	INSPECTED("점검완료"),
	/** 고장 등으로 판매 불가 */
	OUT_OF_ORDER("사용불가");

	private final String label;
}
