package io.github.jeongkyuchoi.hotel.erp.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 스프링 MVC 부가 설정.
 *
 * <p>{@code @EnableWebMvc} 를 붙이지 않는다. 붙이는 순간 스프링 부트의 MVC 자동 구성이
 * 통째로 꺼져서 정적 리소스 매핑, 메시지 컨버터, 뷰 리졸버 설정을 전부 직접 해야 한다.
 * {@link WebMvcConfigurer} 구현만으로 필요한 부분만 덧붙이는 것이 맞다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	/**
	 * 로직 없이 화면만 반환하는 경로를 등록한다.
	 *
	 * <p>로그인 페이지는 보여주기만 하면 되고(인증 처리는 스프링 시큐리티의 필터가 한다),
	 * 이런 경우에 빈 컨트롤러 클래스를 만드는 것은 낭비다.
	 */
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/login").setViewName("login");

		// ---------------------------------------------------------------------
		// React SPA 라우팅 폴백 (D-035)
		//
		// 배포 시 React 빌드는 classpath:/static 에서 서빙된다(build.gradle 의 bootJar).
		// 그런데 SPA 의 클라이언트 라우트(/book, /payment/success …)는 서버에 대응하는
		// 정적 파일이 없다 — 그 경로로 직접 진입·새로고침·결제 리다이렉트가 오면 스프링은
		// 404 를 낸다. 그래서 이 경로들을 index.html 로 forward 해 React Router 가 잡게 한다.
		// ('/' 는 스프링이 static/index.html 을 welcome page 로 자동 서빙하므로 불필요.)
		//
		// **명시적 등록만 한다(광범위 catch-all 금지).** 이 앱은 Thymeleaf 백오피스
		// (/admin, /login)와 SPA 가 공존한다. 무엇이든 index.html 로 넘기는 규칙을 두면
		// 백오피스 경로·/api·/error 까지 삼켜 버린다. SPA 라우트가 늘면 여기에 추가한다.
		// forward 는 query string 을 보존하므로 결제 리다이렉트의 파라미터가 유지된다.
		// ---------------------------------------------------------------------
		registry.addViewController("/book").setViewName("forward:/index.html");
		registry.addViewController("/payment/success").setViewName("forward:/index.html");
		registry.addViewController("/payment/fail").setViewName("forward:/index.html");
		registry.addViewController("/lookup").setViewName("forward:/index.html");
	}
}
