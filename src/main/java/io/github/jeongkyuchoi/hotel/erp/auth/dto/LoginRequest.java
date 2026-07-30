package io.github.jeongkyuchoi.hotel.erp.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 로그인 요청(D-009). */
public record LoginRequest(
		@NotBlank(message = "이메일은 필수입니다.")
		String email,

		@NotBlank(message = "비밀번호는 필수입니다.")
		String password) {
}
