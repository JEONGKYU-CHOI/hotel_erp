package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.RoomTypeForm;
import io.github.jeongkyuchoi.hotel.erp.backoffice.service.RoomTypeService;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
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

/**
 * 객실타입 기준정보 화면.
 *
 * <p>경로가 {@code /admin/basedata/**} 라서 SecurityConfig 에 의해 ADMIN 권한이 필요하다.
 * 프론트데스크(STAFF)는 기준정보를 바꾸지 않는다.
 *
 * <p><b>등록/수정 후 리다이렉트하는 이유 (PRG 패턴)</b> — POST 처리 결과로 화면을 바로
 * 그리면, 사용자가 새로고침할 때 브라우저가 POST 를 다시 보낸다. 같은 등록이 두 번
 * 일어나는 것이다. 처리 후 GET 으로 넘겨 두면 새로고침해도 조회만 반복된다.
 */
@Controller
@RequestMapping("/admin/basedata/room-types")
@RequiredArgsConstructor
public class RoomTypeAdminController {

	private final RoomTypeService roomTypeService;

	@GetMapping
	public String list(Model model) {
		model.addAttribute("roomTypes", roomTypeService.findAll());
		return "admin/basedata/room-type/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("form", new RoomTypeForm());
		model.addAttribute("editing", false);
		return "admin/basedata/room-type/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("form") RoomTypeForm form,
			BindingResult binding, Model model, RedirectAttributes redirect) {

		// 코드 중복은 DB 유니크 제약이 최종적으로 막지만, 그때는 예외가 터진 뒤라
		// 사용자에게 "어느 항목이 문제인지" 돌려주기 어렵다. 먼저 검사해 폼에 붙인다.
		if (!binding.hasFieldErrors("code") && roomTypeService.isCodeTaken(form.getCode())) {
			binding.addError(new FieldError("form", "code", form.getCode(), false, null, null,
					"이미 사용 중인 코드입니다."));
		}
		if (binding.hasErrors()) {
			model.addAttribute("editing", false);
			return "admin/basedata/room-type/form";
		}

		roomTypeService.create(form);
		redirect.addFlashAttribute("flashSuccess",
				"객실타입 '" + form.getName() + "' 을(를) 등록했습니다.");
		return "redirect:/admin/basedata/room-types";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		RoomType roomType = roomTypeService.get(id);
		model.addAttribute("form", RoomTypeForm.from(roomType));
		model.addAttribute("roomTypeId", id);
		model.addAttribute("editing", true);
		return "admin/basedata/room-type/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("form") RoomTypeForm form,
			BindingResult binding, Model model, RedirectAttributes redirect) {

		if (binding.hasErrors()) {
			model.addAttribute("roomTypeId", id);
			model.addAttribute("editing", true);
			return "admin/basedata/room-type/form";
		}

		roomTypeService.update(id, form);
		redirect.addFlashAttribute("flashSuccess",
				"객실타입 '" + form.getName() + "' 을(를) 수정했습니다.");
		return "redirect:/admin/basedata/room-types";
	}

	/**
	 * 판매 재개/중단.
	 *
	 * <p>GET 이 아니라 POST 다. 상태를 바꾸는 요청을 GET 으로 두면 링크를 누르거나
	 * 브라우저가 미리 읽어보는 것만으로 데이터가 바뀔 수 있고, CSRF 방어도 받지 못한다.
	 */
	@PostMapping("/{id}/toggle-active")
	public String toggleActive(@PathVariable Long id, RedirectAttributes redirect) {
		boolean active = roomTypeService.toggleActive(id);
		redirect.addFlashAttribute("flashSuccess",
				active ? "판매를 재개했습니다." : "판매를 중단했습니다.");
		return "redirect:/admin/basedata/room-types";
	}
}
