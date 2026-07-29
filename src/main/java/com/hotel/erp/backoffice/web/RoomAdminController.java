package com.hotel.erp.backoffice.web;

import com.hotel.erp.backoffice.dto.RoomForm;
import com.hotel.erp.backoffice.service.RoomService;
import com.hotel.erp.backoffice.service.RoomTypeService;
import com.hotel.erp.common.domain.basedata.CleanStatus;
import com.hotel.erp.common.domain.basedata.Room;
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

import jakarta.validation.Valid;

/** 호실 기준정보 화면. */
@Controller
@RequestMapping("/admin/basedata/rooms")
@RequiredArgsConstructor
public class RoomAdminController {

	private final RoomService roomService;
	private final RoomTypeService roomTypeService;

	/**
	 * 청결 상태 선택지.
	 *
	 * <p>{@code @ModelAttribute} 로 두면 이 컨트롤러의 모든 화면에 자동으로 담겨,
	 * 검증 실패로 폼을 다시 그릴 때 채우는 것을 깜빡할 수 없다.
	 * enum 상수라 DB 조회가 없으므로 목록 화면에 함께 실려도 비용이 없다.
	 */
	@ModelAttribute("cleanStatuses")
	public CleanStatus[] cleanStatuses() {
		return CleanStatus.values();
	}

	/**
	 * 객실타입 드롭다운 채우기.
	 *
	 * <p>이것도 {@code @ModelAttribute} 로 두었다가 되돌렸다. 그러면 드롭다운을 쓰지도
	 * 않는 <b>목록 화면에서까지</b> {@code room_type} 조회가 매번 나간다. 실제로 로그로
	 * 확인했다 — 호실 목록 요청 하나에 SQL 이 2건이었고 그중 하나가 이것이었다.
	 *
	 * <p>대신 폼을 반환하는 모든 경로가 이 메서드를 거치게 해서, 검증 실패 후 다시
	 * 그릴 때 드롭다운이 비는 실수를 막는다.
	 */
	private String formView(Model model, boolean editing) {
		model.addAttribute("roomTypes", roomTypeService.findAll());
		model.addAttribute("editing", editing);
		return "admin/basedata/room/form";
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("rooms", roomService.findAll());
		return "admin/basedata/room/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("form", new RoomForm());
		return formView(model, false);
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("form") RoomForm form,
			BindingResult binding, Model model, RedirectAttributes redirect) {

		if (!binding.hasFieldErrors("roomNo") && roomService.isRoomNoTaken(form.getRoomNo())) {
			binding.addError(new FieldError("form", "roomNo", form.getRoomNo(), false, null, null,
					"이미 사용 중인 호실 번호입니다."));
		}
		if (binding.hasErrors()) {
			return formView(model, false);
		}

		roomService.create(form);
		redirect.addFlashAttribute("flashSuccess",
				form.getRoomNo() + "호를 등록했습니다.");
		return "redirect:/admin/basedata/rooms";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Room room = roomService.get(id);
		model.addAttribute("form", RoomForm.from(room));
		model.addAttribute("roomId", id);
		model.addAttribute("occupancyStatus", room.getOccupancyStatus());
		return formView(model, true);
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("form") RoomForm form,
			BindingResult binding, Model model, RedirectAttributes redirect) {

		if (!binding.hasFieldErrors("roomNo")
				&& roomService.isRoomNoTakenByOther(form.getRoomNo(), id)) {
			binding.addError(new FieldError("form", "roomNo", form.getRoomNo(), false, null, null,
					"이미 사용 중인 호실 번호입니다."));
		}
		if (binding.hasErrors()) {
			model.addAttribute("roomId", id);
			model.addAttribute("occupancyStatus", roomService.get(id).getOccupancyStatus());
			return formView(model, true);
		}

		roomService.update(id, form);
		redirect.addFlashAttribute("flashSuccess", form.getRoomNo() + "호를 수정했습니다.");
		return "redirect:/admin/basedata/rooms";
	}

	@PostMapping("/{id}/toggle-active")
	public String toggleActive(@PathVariable Long id, RedirectAttributes redirect) {
		boolean active = roomService.toggleActive(id);
		redirect.addFlashAttribute("flashSuccess",
				active ? "호실을 사용 상태로 되돌렸습니다." : "호실을 사용 중지했습니다.");
		return "redirect:/admin/basedata/rooms";
	}
}
