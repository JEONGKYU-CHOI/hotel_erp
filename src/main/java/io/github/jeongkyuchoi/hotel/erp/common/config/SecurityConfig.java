package io.github.jeongkyuchoi.hotel.erp.common.config;

import io.github.jeongkyuchoi.hotel.erp.common.security.JwtAuthenticationFilter;
import io.github.jeongkyuchoi.hotel.erp.common.security.JwtProperties;
import io.github.jeongkyuchoi.hotel.erp.common.security.JwtTokenProvider;
import io.github.jeongkyuchoi.hotel.erp.common.security.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 보안 설정.
 *
 * <p><b>필터 체인을 둘로 나눈 이유</b> — 이 앱은 성격이 다른 두 개의 프론트를 함께
 * 서비스한다(D-001).
 *
 * <table border="1">
 *   <caption>영역별 인증 방식</caption>
 *   <tr><th>영역</th><th>인증</th><th>세션</th><th>CSRF</th></tr>
 *   <tr><td>{@code /api/**} 부킹엔진 REST</td><td>JWT (D-008)</td><td>없음</td><td>불필요</td></tr>
 *   <tr><td>그 외 (백오피스 + 정적 리소스)</td><td>세션 쿠키 + 폼 로그인</td><td>사용</td><td>필요</td></tr>
 * </table>
 *
 * <p>한 체인에 몰아넣고 경로별로 분기하면 "이 경로는 세션을 쓰나 안 쓰나"가
 * 설정 안에서 흐려진다. 체인을 나누면 각 체인이 통째로 하나의 정책이 된다.
 *
 * <p><b>CSRF 를 API 체인에서만 끄는 이유</b> — CSRF 공격은 브라우저가 <b>쿠키를
 * 자동으로 실어 보내는 것</b>을 악용한다. JWT 를 헤더로 보내는 무상태 API 는 쿠키가
 * 없으니 성립하지 않는다. 반대로 세션 쿠키를 쓰는 백오피스는 반드시 켜 두어야 한다.
 * Thymeleaf 의 {@code <form>} 은 토큰을 자동으로 넣어 준다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	/**
	 * ① 부킹엔진 REST API 체인 (JWT, D-008).
	 *
	 * <p><b>인증은 선택이다.</b> 비회원 예약 경로(가용·HOLD·조회)는 인증 없이 열어 두고
	 * (D-008), 회원 전용 경로({@code /api/me} 등)만 JWT 를 요구한다. JWT 필터는 토큰이 있으면
	 * 인증 컨텍스트를 채우고, 없거나 무효여도 요청을 막지 않는다 — 보호 경로는 인가 단계와
	 * {@link RestAuthenticationEntryPoint}(401)가 막는다.
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtTokenProvider tokenProvider)
			throws Exception {
		return http
				// 이 체인이 담당할 경로를 못 박는다. 여기에 걸리지 않은 요청은
				// 아래 기본 체인으로 넘어간다.
				.securityMatcher("/api/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// 무상태 API 가 로그인 폼으로 리다이렉트하면 클라이언트는 HTML 을 받는다.
				// 폼 로그인과 기본 인증을 모두 꺼서 401 이 그대로 나가게 한다.
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.authorizeHttpRequests(auth -> auth
						// 회원가입·로그인은 인증 전 경로다.
						.requestMatchers("/api/auth/**").permitAll()
						// 비회원 예약 경로 — 인증 없이 계속 동작한다(D-008).
						.requestMatchers(HttpMethod.GET, "/api/room-types").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/availability").permitAll()
						.requestMatchers("/api/reservations/**").permitAll()
						// 결제 승인 콜백·토스 웹훅 — 인증 주체가 없는 경로다(D-034). 승인은
						// paymentKey·금액 대조·확정 서비스 멱등으로, 웹훅은 멱등·금액 대조로 보호한다.
						.requestMatchers("/api/payments/**").permitAll()
						// 그 밖(/api/me 등)은 회원 인증 필요. 새 경로가 기본 '허용'으로
						// 새지 않도록 마지막을 authenticated 로 못 박는다.
						.anyRequest().authenticated())
				// 무효/부재 토큰이면 인증 없이 넘기고, 보호 경로에서 401 로 막는다.
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
				.addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
						UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	/**
	 * ② 백오피스 + 부킹엔진 화면 체인 (기본).
	 */
	@Bean
	public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(auth -> auth
						// 부킹엔진(React) 진입점과 빌드 산출물. 고객은 로그인 없이 들어온다.
						.requestMatchers("/", "/index.html", "/assets/**",
								"/favicon.svg", "/icons.svg").permitAll()
						// 결제 리다이렉트 수신 경로 (vite.config.js 의 프록시 대상)
						.requestMatchers("/payments/**").permitAll()
						// 기동 확인용. 상세 정보는 인증된 사용자에게만 보인다(application.yml).
						.requestMatchers("/actuator/health").permitAll()
						// /error 를 열어두지 않으면 모든 404 가 로그인 페이지로 튕긴다.
						// 스프링은 처리기가 없는 요청을 내부적으로 /error 로 다시 보내는데(ERROR
						// dispatch), 그 경로가 인증 대상이면 302 가 나간다. 실제로 겪었다 —
						// 인증 없이 열어둔 /api/** 조차 404 상황에서 로그인으로 리다이렉트됐다.
						.requestMatchers("/error").permitAll()
						// 기준정보 변경은 관리자만. 프론트데스크(STAFF)는 예약·체크인 담당이다.
						.requestMatchers("/admin/basedata/**").hasRole("ADMIN")
						.requestMatchers("/admin/**").authenticated()
						// 위에서 명시하지 않은 경로는 전부 막는다. 새 경로를 추가했을 때
						// 기본값이 '허용'이면 인증을 빠뜨려도 아무도 모른다.
						.anyRequest().authenticated())
				.formLogin(form -> form
						// 스프링 기본 폼 대신 우리 로그인 페이지를 쓴다(templates/login.html).
						// 이 경로를 처리할 핸들러는 WebMvcConfig 의 뷰 컨트롤러가 제공한다.
						.loginPage("/login")
						// 로그인 성공 후 기본 착지점. 원래 가려던 곳이 있으면 그쪽이 우선한다.
						.defaultSuccessUrl("/admin", false)
						.failureUrl("/login?error")
						.permitAll())
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll())
				.build();
	}

	/**
	 * 비밀번호 해시.
	 *
	 * <p>BCrypt 는 <b>일부러 느리게</b> 설계된 해시다. SHA-256 같은 범용 해시는 너무 빨라서
	 * 유출 시 초당 수억 건을 대입해 볼 수 있다. 또 해시마다 무작위 salt 가 자동으로 들어가
	 * 같은 비밀번호라도 저장값이 매번 다르다.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
