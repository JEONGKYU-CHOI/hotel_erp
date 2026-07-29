package com.hotel.erp.backoffice.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 백오피스 진입점.
 *
 * <p>로그인 성공 후 착지하는 곳이다(SecurityConfig 의 defaultSuccessUrl).
 * 지금은 안내만 띄우고, 대시보드(오늘 도착/출발, 재고 현황)는 예약 모듈이 생긴 뒤에 채운다.
 */
@Controller
public class AdminHomeController {

	@GetMapping("/admin")
	public String home() {
		return "admin/index";
	}
}
