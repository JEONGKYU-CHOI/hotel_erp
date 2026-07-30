package io.github.jeongkyuchoi.hotel.erp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청(D-009).
 *
 * <p>비밀번호는 평문으로 여기까지만 온다 — 서비스가 즉시 BCrypt 로 해싱하고, 이 값은
 * 로그·응답 어디에도 남기지 않는다.
 */
public record SignupRequest(
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		String email,

		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다.")
		String password,

		@NotBlank(message = "이름은 필수입니다.")
		String name,

		@NotBlank(message = "연락처는 필수입니다.")
		String phone) {
}
