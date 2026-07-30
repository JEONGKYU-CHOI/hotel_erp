package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.backoffice.service.ReservationAdminService;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 백오피스 예약 목록·상세 화면(D-029).
 *
 * <p>{@code /admin/reservations} 는 {@code /admin/basedata/**}(ADMIN 전용)가 아니라
 * {@code /admin/**} 의 일반 인증 경로다 — 예약 확인은 프론트데스크(STAFF)의 일상 업무다
 * (SecurityConfig 참조).
 */
@Controller
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
public class ReservationAdminController {

	private final ReservationAdminService reservationAdminService;

	/** 상태 필터 드롭다운 선택지. enum 상수라 DB 조회가 없어 모든 화면에 실려도 비용이 없다. */
	@ModelAttribute("statuses")
	public ReservationStatus[] statuses() {
		return ReservationStatus.values();
	}

	/**
	 * 예약 목록.
	 *
	 * <p><b>{@code status} 를 {@code ReservationStatus} 가 아니라 {@code String} 으로 받는
	 * 이유</b> — 필터 드롭다운의 "전체" 선택지는 빈 문자열이다. 빈 문자열을 enum 파라미터로
	 * 바로 바인딩하면 변환 실패로 400 이 난다. 문자열로 받아 비어 있으면 null(필터 없음)로,
	 * 값이 있으면 enum 으로 파싱한다.
	 */
	@GetMapping
	public String list(
			@RequestParam(required = false) String status,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			Model model) {

		ReservationStatus statusFilter = parseStatus(status);

		model.addAttribute("reservations", reservationAdminService.list(statusFilter, from, to));
		model.addAttribute("selectedStatus", statusFilter);
		model.addAttribute("from", from);
		model.addAttribute("to", to);
		return "admin/reservation/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("r", reservationAdminService.get(id));
		return "admin/reservation/detail";
	}

	/** 빈 문자열·미지의 값은 필터 없음(null)으로 취급한다. */
	private ReservationStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return ReservationStatus.valueOf(status);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
