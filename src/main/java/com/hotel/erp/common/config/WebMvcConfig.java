package com.hotel.erp.common.config;

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
	}
}
