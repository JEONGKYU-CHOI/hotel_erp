package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.backoffice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 백오피스 진입점 — 대시보드.
 *
 * <p>로그인 성공 후 착지하는 곳이다(SecurityConfig 의 defaultSuccessUrl). 오늘의 도착·출발·
 * 재실·하우스키핑·최근 예약을 {@link DashboardService} 로 모아 한 화면에 보인다.
 */
@Controller
@RequiredArgsConstructor
public class AdminHomeController {

	private final DashboardService dashboardService;

	@GetMapping("/admin")
	public String home(Model model) {
		model.addAttribute("dash", dashboardService.load());
		return "admin/index";
	}
}
