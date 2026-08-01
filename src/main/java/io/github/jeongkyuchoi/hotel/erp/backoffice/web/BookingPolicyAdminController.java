package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.BookingPolicyForm;
import io.github.jeongkyuchoi.hotel.erp.backoffice.service.BookingPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 예약 정책 화면. 경로가 {@code /admin/basedata/**} 라 SecurityConfig 에 의해 ADMIN 전용이다.
 *
 * <p>단일 행 설정이라 목록이 없다 — 편집 폼 하나를 GET 으로 보여주고 POST 로 저장한다.
 * 저장 후 PRG(리다이렉트)로 새로고침 재전송을 막는다(다른 기준정보 화면과 같은 규율).
 */
@Controller
@RequestMapping("/admin/basedata/booking-policy")
@RequiredArgsConstructor
public class BookingPolicyAdminController {

	private final BookingPolicyService bookingPolicyService;

	@GetMapping
	public String edit(Model model) {
		model.addAttribute("form", BookingPolicyForm.from(bookingPolicyService.getOrCreate()));
		return "admin/basedata/booking-policy/form";
	}

	@PostMapping
	public String update(@Valid @ModelAttribute("form") BookingPolicyForm form,
			BindingResult binding, RedirectAttributes redirect) {

		if (binding.hasErrors()) {
			return "admin/basedata/booking-policy/form";
		}

		bookingPolicyService.updateSameDayCutoff(form.getSameDayCutoffTime());
		redirect.addFlashAttribute("flashSuccess", "예약 정책을 저장했습니다.");
		return "redirect:/admin/basedata/booking-policy";
	}
}
