package io.github.jeongkyuchoi.hotel.erp.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer 토큰을 검증해 인증 컨텍스트를 채우는 필터 (D-008).
 *
 * <p><b>이 필터는 인증하지 못해도 요청을 막지 않는다.</b> 토큰이 없거나 무효면 그냥 인증 없이
 * 넘긴다 — 비회원 예약 경로(availability·hold·lookup)가 인증 없이 계속 동작해야 하기
 * 때문이다(D-008). 보호된 경로는 인가 단계에서 걸러지고, 그때 {@link RestAuthenticationEntryPoint}
 * 가 401 을 돌려준다.
 *
 * <p>스프링 빈으로 등록하지 않고 {@code SecurityConfig} 에서 직접 생성해 API 체인에만 끼운다
 * — 빈으로 두면 Boot 가 서블릿 필터로도 자동 등록해 모든 요청에 이중으로 돈다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";
	private static final List<SimpleGrantedAuthority> MEMBER_AUTHORITY =
			List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));

	private final JwtTokenProvider tokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		String header = request.getHeader(HEADER);
		if (header != null && header.startsWith(PREFIX)) {
			String token = header.substring(PREFIX.length());
			try {
				Long memberId = tokenProvider.parseMemberId(token);
				var authentication = new UsernamePasswordAuthenticationToken(
						memberId, null, MEMBER_AUTHORITY);
				authentication.setDetails(
						new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (RuntimeException e) {
				// 무효·만료 토큰. 인증 컨텍스트를 비우고 넘긴다 — 보호 경로면 엔트리포인트가 401.
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}
}
