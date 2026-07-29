package com.hotel.erp.common.domain.member;

/** 고객 회원 상태. */
public enum MemberStatus {
	/** 정상 */
	ACTIVE,
	/** 장기 미접속 휴면 */
	DORMANT,
	/**
	 * 탈퇴.
	 *
	 * <p>행을 지우지 않는다. 과거 예약이 이 회원을 참조하고 있어 삭제하면 FK 가 깨지고,
	 * 정산·감사 기록도 함께 사라진다. 개인정보는 별도로 마스킹한다.
	 */
	WITHDRAWN
}
