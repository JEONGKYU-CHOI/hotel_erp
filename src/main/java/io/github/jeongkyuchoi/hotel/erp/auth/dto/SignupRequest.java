package io.github.jeongkyuchoi.hotel.erp.auth.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 회원가입 요청(D-009).
 *
 * <p>비밀번호는 평문으로 여기까지만 온다 — 서비스가 즉시 BCrypt 로 해싱하고, 이 값은
 * 로그·응답 어디에도 남기지 않는다.
 *
 * <p>성별·생년월일은 추후 나이·성별 기반 추천 메일의 근거 데이터로 받는다.
 */
public record SignupRequest(
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		String email,

		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(max = 72, message = "비밀번호가 너무 깁니다.")
		@Pattern(regexp = "^(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
				message = "비밀번호는 특수문자와 숫자를 포함해 8자 이상이어야 합니다.")
		String password,

		@NotBlank(message = "이름은 필수입니다.")
		String name,

		@NotBlank(message = "연락처는 필수입니다.")
		@Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "휴대전화는 010-1234-5678 형식으로 입력하세요.")
		String phone,

		@NotNull(message = "성별은 필수입니다.")
		Gender gender,

		@NotNull(message = "생년월일은 필수입니다.")
		@Past(message = "생년월일은 과거 날짜여야 합니다.")
		LocalDate birthDate,

		/** 마케팅 수신 동의(선택). 미지정이면 미동의로 본다. */
		Boolean marketingConsent) {

	/** null-safe 동의 여부. */
	public boolean marketingConsentOrFalse() {
		return Boolean.TRUE.equals(marketingConsent);
	}
}
