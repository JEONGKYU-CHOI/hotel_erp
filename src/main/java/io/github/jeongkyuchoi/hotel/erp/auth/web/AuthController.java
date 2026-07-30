package io.github.jeongkyuchoi.hotel.erp.auth.web;

import io.github.jeongkyuchoi.hotel.erp.auth.dto.LoginRequest;
import io.github.jeongkyuchoi.hotel.erp.auth.dto.MeResponse;
import io.github.jeongkyuchoi.hotel.erp.auth.dto.SignupRequest;
import io.github.jeongkyuchoi.hotel.erp.auth.dto.TokenResponse;
import io.github.jeongkyuchoi.hotel.erp.auth.service.MemberAuthService;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Member;
import io.github.jeongkyuchoi.hotel.erp.common.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 인증 — 회원가입·로그인 (D-008, D-009). 인증 전 경로다(SecurityConfig 에서 permitAll).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final MemberAuthService memberAuthService;
	private final JwtTokenProvider tokenProvider;

	/** 회원가입. 가입 즉시 로그인시키지 않고 201 만 돌려준다 — 로그인은 다음 단계다. */
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public MeResponse signup(@Valid @RequestBody SignupRequest request) {
		return MeResponse.from(memberAuthService.signup(request));
	}

	/** 로그인. 자격증명을 검증하고 Access Token 을 발급한다(D-010, 리프레시 없음). */
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		Member member = memberAuthService.authenticate(request);
		String token = tokenProvider.issue(member.getId(), member.getEmail());
		return TokenResponse.bearer(token, tokenProvider.validitySeconds());
	}
}
