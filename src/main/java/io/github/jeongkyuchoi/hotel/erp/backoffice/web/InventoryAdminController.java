package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.InventoryGenerateForm;
import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.InventoryGenerateResult;
import io.github.jeongkyuchoi.hotel.erp.backoffice.service.InventoryService;
import io.github.jeongkyuchoi.hotel.erp.backoffice.service.RoomTypeService;
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
 * 재고 현황 · 생성 화면.
 *
 * <p>경로가 {@code /admin/inventory} 라 기준정보({@code /admin/basedata/**})와 달리
 * ADMIN 전용이 아니다. 재고 확인은 프론트데스크의 일상 업무다.
 */
@Controller
@RequiredArgsConstructor
public class InventoryAdminController {

	private final InventoryService inventoryService;
	private final RoomTypeService roomTypeService;

	@ModelAttribute("roomTypes")
	public Object roomTypes() {
		return roomTypeService.findAll();
	}

	/**
	 * 재고 현황.
	 *
	 * <p>기간을 지정하지 않으면 오늘부터 2주를 본다. 프론트데스크가 화면을 열자마자
	 * 확인하고 싶은 것이 대개 그 범위다.
	 */
	@GetMapping("/admin/inventory")
	public String list(
			@RequestParam(required = false) Long roomTypeId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			Model model) {

		LocalDate fromDate = from != null ? from : LocalDate.now();
		LocalDate toDate = to != null ? to : fromDate.plusDays(13);

		model.addAttribute("from", fromDate);
		model.addAttribute("to", toDate);
		model.addAttribute("roomTypeId", roomTypeId);

		if (roomTypeId != null) {
			model.addAttribute("inventories",
					inventoryService.findRange(roomTypeId, fromDate, toDate));
			model.addAttribute("activeRoomCount", inventoryService.countActiveRooms(roomTypeId));
		}
		return "admin/inventory/list";
	}

	@GetMapping("/admin/inventory/generate")
	public String generateForm(@RequestParam(required = false) Long roomTypeId, Model model) {
		InventoryGenerateForm form = new InventoryGenerateForm();
		form.setRoomTypeId(roomTypeId);
		form.setFromDate(LocalDate.now());
		form.setToDate(LocalDate.now().plusDays(29));
		model.addAttribute("form", form);
		return "admin/inventory/generate";
	}

	@PostMapping("/admin/inventory/generate")
	public String generate(@Valid @ModelAttribute("form") InventoryGenerateForm form,
			BindingResult binding, RedirectAttributes redirect) {

		if (binding.hasErrors()) {
			return "admin/inventory/generate";
		}

		InventoryGenerateResult result = inventoryService.generate(form);

		redirect.addFlashAttribute("flashSuccess", String.format(
				"재고 %d일 처리: 생성 %d · 갱신 %d · 건너뜀 %d",
				result.total(), result.created(), result.updated(), result.skipped().size()));
		// 건너뛴 날짜는 사유와 함께 별도로 보여준다. 성공 메시지에 묻히면 놓친다.
		if (result.hasSkipped()) {
			redirect.addFlashAttribute("skipped", result.skipped());
		}

		return "redirect:/admin/inventory?roomTypeId=" + form.getRoomTypeId()
				+ "&from=" + form.getFromDate() + "&to=" + form.getToDate();
	}
}
