package com.hotel.erp.backoffice.web;

import com.hotel.erp.backoffice.dto.RateCalendarGenerateForm;
import com.hotel.erp.backoffice.dto.RateCalendarGenerateResult;
import com.hotel.erp.backoffice.service.RateCalendarService;
import com.hotel.erp.backoffice.service.RatePlanService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 요금 캘린더 현황 · 생성 화면.
 *
 * <p>경로가 {@code /admin/basedata/**} 라 ADMIN 전용이다(SecurityConfig). 요금 설정은
 * 기준정보 담당의 일이지 프론트데스크의 일상 업무가 아니다 — 이 점이 재고 화면
 * ({@code /admin/inventory}, STAFF 도 접근 가능)과 다르다.
 */
@Controller
@RequiredArgsConstructor
public class RateCalendarAdminController {

	private final RateCalendarService rateCalendarService;
	private final RatePlanService ratePlanService;

	/** 요금정책 선택지. 폼·목록 양쪽에서 쓴다. */
	@ModelAttribute("ratePlans")
	public Object ratePlans() {
		return ratePlanService.findAll();
	}

	@GetMapping("/admin/basedata/rate-calendar")
	public String list(
			@RequestParam(required = false) Long ratePlanId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			Model model) {

		LocalDate fromDate = from != null ? from : LocalDate.now();
		LocalDate toDate = to != null ? to : fromDate.plusDays(29);

		model.addAttribute("from", fromDate);
		model.addAttribute("to", toDate);
		model.addAttribute("ratePlanId", ratePlanId);

		if (ratePlanId != null) {
			model.addAttribute("rates",
					rateCalendarService.findRange(ratePlanId, fromDate, toDate));
		}
		return "admin/basedata/rate-calendar/list";
	}

	@GetMapping("/admin/basedata/rate-calendar/generate")
	public String generateForm(@RequestParam(required = false) Long ratePlanId, Model model) {
		RateCalendarGenerateForm form = new RateCalendarGenerateForm();
		form.setRatePlanId(ratePlanId);
		form.setFromDate(LocalDate.now());
		form.setToDate(LocalDate.now().plusDays(29));
		model.addAttribute("form", form);
		return "admin/basedata/rate-calendar/generate";
	}

	@PostMapping("/admin/basedata/rate-calendar/generate")
	public String generate(@Valid @ModelAttribute("form") RateCalendarGenerateForm form,
			BindingResult binding, RedirectAttributes redirect) {

		if (binding.hasErrors()) {
			return "admin/basedata/rate-calendar/generate";
		}

		RateCalendarGenerateResult result = rateCalendarService.generate(form);

		redirect.addFlashAttribute("flashSuccess", String.format(
				"요금 %d일 처리: 생성 %d · 갱신 %d · 유지 %d",
				result.total(), result.created(), result.updated(), result.unchanged()));

		return "redirect:/admin/basedata/rate-calendar?ratePlanId=" + form.getRatePlanId()
				+ "&from=" + form.getFromDate() + "&to=" + form.getToDate();
	}
}
