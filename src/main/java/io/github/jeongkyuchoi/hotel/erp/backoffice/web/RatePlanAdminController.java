package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.RatePlanForm;
import io.github.jeongkyuchoi.hotel.erp.backoffice.service.RatePlanService;
import io.github.jeongkyuchoi.hotel.erp.backoffice.service.RoomTypeService;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 요금정책 기준정보 화면. */
@Controller
@RequestMapping("/admin/basedata/rate-plans")
@RequiredArgsConstructor
public class RatePlanAdminController {

	private final RatePlanService ratePlanService;
	private final RoomTypeService roomTypeService;

	/**
	 * 객실타입 드롭다운을 채워 폼 뷰 이름을 돌려준다.
	 *
	 * <p>{@code @ModelAttribute} 로 컨트롤러 전역에 두지 않는 이유는
	 * {@code RoomAdminController.formView} 주석과 같다 — 드롭다운을 쓰지 않는
	 * 목록 화면에서까지 조회가 나간다.
	 */
	private String formView(Model model, boolean editing) {
		model.addAttribute("roomTypes", roomTypeService.findAll());
		model.addAttribute("editing", editing);
		return "admin/basedata/rate-plan/form";
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("ratePlans", ratePlanService.findAll());
		return "admin/basedata/rate-plan/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("form", new RatePlanForm());
		return formView(model, false);
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("form") RatePlanForm form,
			BindingResult binding, Model model, RedirectAttributes redirect) {

		if (!binding.hasFieldErrors("code") && ratePlanService.isCodeTaken(form.getCode())) {
			binding.addError(new FieldError("form", "code", form.getCode(), false, null, null,
					"이미 사용 중인 코드입니다."));
		}
		if (binding.hasErrors()) {
			return formView(model, false);
		}

		ratePlanService.create(form);
		redirect.addFlashAttribute("flashSuccess",
				"요금정책 '" + form.getName() + "' 을(를) 등록했습니다.");
		return "redirect:/admin/basedata/rate-plans";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		RatePlan ratePlan = ratePlanService.get(id);
		model.addAttribute("form", RatePlanForm.from(ratePlan));
		model.addAttribute("ratePlanId", id);
		return formView(model, true);
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("form") RatePlanForm form,
			BindingResult binding, Model model, RedirectAttributes redirect) {

		if (binding.hasErrors()) {
			model.addAttribute("ratePlanId", id);
			return formView(model, true);
		}

		ratePlanService.update(id, form);
		redirect.addFlashAttribute("flashSuccess",
				"요금정책 '" + form.getName() + "' 을(를) 수정했습니다.");
		return "redirect:/admin/basedata/rate-plans";
	}

	@PostMapping("/{id}/toggle-active")
	public String toggleActive(@PathVariable Long id, RedirectAttributes redirect) {
		boolean active = ratePlanService.toggleActive(id);
		redirect.addFlashAttribute("flashSuccess",
				active ? "판매를 재개했습니다." : "판매를 중단했습니다.");
		return "redirect:/admin/basedata/rate-plans";
	}
}
