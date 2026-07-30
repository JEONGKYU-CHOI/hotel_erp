package io.github.jeongkyuchoi.hotel.erp.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 미인증 요청에 대한 API 응답 (D-008).
 *
 * <p>무상태 API 는 로그인 폼으로 리다이렉트하면 안 된다 — 클라이언트는 HTML 을 받게 된다.
 * 401 과 JSON 본문을 그대로 돌려준다. {@code ApiError}(booking 패키지)와 형태를 맞추되,
 * 보안 패키지가 웹 DTO 에 의존하지 않도록 본문은 직접 쓴다.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}");
	}
}
