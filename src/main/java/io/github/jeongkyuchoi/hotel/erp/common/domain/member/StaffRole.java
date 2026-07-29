package io.github.jeongkyuchoi.hotel.erp.common.domain.member;

/**
 * 백오피스 직원 권한.
 *
 * <p>고객({@link Member})과 테이블이 분리돼 있어(D-014) 이 권한이 고객 계정에
 * 붙을 수 있는 경로가 스키마 수준에서 없다.
 */
public enum StaffRole {
	/** 전체 권한. 기준정보 변경, 마감 수동 실행 */
	ADMIN,
	/** 프론트데스크 업무. 예약·체크인·체크아웃 */
	STAFF
}
