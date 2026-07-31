package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.backoffice.service.HousekeepingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 하우스키핑 현황판 (D-040). 청소 대상 호실을 띄우고 청소 시작·완료를 처리한다.
 *
 * <p>{@code /admin/**} 의 일반 인증 경로다 — 청소 처리는 프론트데스크/하우스키핑(STAFF)의
 * 일상 업무다(SecurityConfig). 상태 오류는 서버에서 막고 오류 플래시로 되돌린다.
 */
@Controller
@RequestMapping("/admin/housekeeping")
@RequiredArgsConstructor
public class HousekeepingController {

	private final HousekeepingService housekeepingService;

	@GetMapping
	public String board(Model model) {
		model.addAttribute("rooms", housekeepingService.board());
		return "admin/housekeeping/board";
	}

	@PostMapping("/{id}/start")
	public String start(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			housekeepingService.startCleaning(id);
			redirect.addFlashAttribute("flashSuccess", "청소를 시작했습니다.");
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("flashError", "청소를 시작할 수 없습니다: " + e.getMessage());
		}
		return "redirect:/admin/housekeeping";
	}

	@PostMapping("/{id}/finish")
	public String finish(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			housekeepingService.finishCleaning(id);
			redirect.addFlashAttribute("flashSuccess", "청소를 완료했습니다.");
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("flashError", "청소를 완료할 수 없습니다: " + e.getMessage());
		}
		return "redirect:/admin/housekeeping";
	}

	@PostMapping("/{id}/inspect")
	public String inspect(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			housekeepingService.inspect(id);
			redirect.addFlashAttribute("flashSuccess", "점검을 완료했습니다.");
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("flashError", "점검할 수 없습니다: " + e.getMessage());
		}
		return "redirect:/admin/housekeeping";
	}
}
