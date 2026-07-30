package io.github.jeongkyuchoi.hotel.erp.auth.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Member;

/**
 * 현재 로그인 회원 정보(D-009). <b>{@code passwordHash} 는 절대 담지 않는다.</b>
 */
public record MeResponse(Long id, String email, String name, String phone) {

	public static MeResponse from(Member m) {
		return new MeResponse(m.getId(), m.getEmail(), m.getName(), m.getPhone());
	}
}
