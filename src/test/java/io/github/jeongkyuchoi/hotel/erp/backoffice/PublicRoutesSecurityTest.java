package io.github.jeongkyuchoi.hotel.erp.backoffice;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 부킹엔진 공개 라우트가 비회원에게 열려 있는지 검증한다.
 *
 * <p><b>왜 필요한가</b> — SPA 콘텐츠 라우트를 추가할 때 세 곳을 함께 고쳐야 한다:
 * React 라우트, {@code WebMvcConfig}(딥링크 forward), 그리고 {@code SecurityConfig}
 * (permitAll). 마지막을 빠뜨리면 {@code anyRequest().authenticated()} 에 걸려 비회원이
 * 로그인으로 302 튕긴다 — 새 페이지(/about·/reviews)에서 실제로 났던 회귀다.
 *
 * <p>프론트 정적 리소스는 bootJar 에만 번들되고 테스트 클래스패스엔 없어, permitAll 인
 * 경로는 forward 대상 부재로 404 가 된다. 그래서 "200"이 아니라 <b>"로그인으로 302 되지
 * 않는다"</b>를 불변식으로 검증한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PublicRoutesSecurityTest {

	@Autowired private MockMvc mockMvc;

	@ParameterizedTest
	@ValueSource(strings = {"/rooms", "/dining", "/facilities", "/location", "/packages",
			"/faq", "/gallery", "/about", "/reviews", "/lookup", "/member-login", "/signup"})
	@WithAnonymousUser
	@DisplayName("공개 콘텐츠 라우트는 비회원 접근에 로그인으로 튕기지 않는다")
	void publicRoutes_notRedirectedToLogin(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(status().is(not(302)));
	}
}
