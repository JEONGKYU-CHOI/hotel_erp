package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.backoffice.service.ReservationAdminService;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.CancellationCharge;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.common.exception.PaymentException;
import io.github.jeongkyuchoi.hotel.erp.folio.service.FolioService;
import io.github.jeongkyuchoi.hotel.erp.payment.service.RefundService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationCancelService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.StayService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
	private final ReservationCancelService reservationCancelService;
	private final StayService stayService;
	private final FolioService folioService;
	private final RefundService refundService;

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
		var detail = reservationAdminService.get(id);
		model.addAttribute("r", detail);
		model.addAttribute("reservationId", id);
		model.addAttribute("folio", folioService.forAdmin(id)); // 청구서 패널(D-038)
		model.addAttribute("history", reservationAdminService.history(id)); // 환불·취소 이력(D-042)
		// 확정 상태면 체크인 호실 선택지를 함께 싣는다.
		if (detail.status() == ReservationStatus.CONFIRMED) {
			model.addAttribute("assignableRooms", reservationAdminService.assignableRoomsFor(id));
		}
		return "admin/reservation/detail";
	}

	/**
	 * 예약 취소. 취소 서비스가 직전 상태에 따라 재고를 되돌린다(D-027).
	 *
	 * <p>취소 불가 상태(CHECKED_IN 등)는 서비스가 {@link IllegalStateException} 으로 거부한다 —
	 * 화면에서 버튼을 숨기지만(표시 상태 기반), 요청을 직접 만들어 보낼 수도 있으므로 서버에서도
	 * 막고 오류 플래시로 되돌린다.
	 */
	@PostMapping("/{id}/cancel")
	public String cancel(@PathVariable Long id,
			@RequestParam(required = false) String reason,
			RedirectAttributes redirect) {
		try {
			CancellationCharge charge = reservationCancelService.cancel(id, reason);
			redirect.addFlashAttribute("flashSuccess", String.format(
					"예약을 취소했습니다. 위약금 %,d원 · 환불 %,d원",
					charge.penalty().longValue(), charge.refund().longValue()));
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("flashError", "취소할 수 없는 상태입니다.");
		}
		return "redirect:/admin/reservations/" + id;
	}

	/**
	 * 환불 실행 — 폴리오 잔액이 환불 대상이면 그 금액만큼 토스 결제취소를 실행한다(D-039).
	 * 돌려줄 것이 없으면 서비스가 무동작한다. 토스 실패는 {@link PaymentException} 으로 올라와
	 * 롤백되고 오류 플래시로 되돌린다 — 실제 돈과 장부가 어긋나지 않는다.
	 */
	@PostMapping("/{id}/refund")
	public String refund(@PathVariable Long id,
			@RequestParam(required = false) String reason,
			RedirectAttributes redirect) {
		try {
			var folio = refundService.refund(id, reason != null ? reason : "고객 환불");
			redirect.addFlashAttribute("flashSuccess",
					String.format("환불을 실행했습니다. 잔액 %,d원", folio.balance().longValue()));
		} catch (PaymentException e) {
			redirect.addFlashAttribute("flashError", "환불에 실패했습니다: " + e.getMessage());
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("flashError", "환불할 수 없습니다: " + e.getMessage());
		}
		return "redirect:/admin/reservations/" + id;
	}

	/**
	 * 체크인 — 확정 예약에 호실을 배정한다(D-031). 배정 불가/상태 오류는 서버에서 막고
	 * 오류 플래시로 되돌린다.
	 */
	@PostMapping("/{id}/check-in")
	public String checkIn(@PathVariable Long id, @RequestParam Long roomId,
			RedirectAttributes redirect) {
		try {
			stayService.checkIn(id, roomId);
			redirect.addFlashAttribute("flashSuccess", "체크인했습니다.");
		} catch (IllegalStateException | IllegalArgumentException e) {
			redirect.addFlashAttribute("flashError", "체크인할 수 없습니다: " + e.getMessage());
		}
		return "redirect:/admin/reservations/" + id;
	}

	/** 체크아웃 — 투숙 예약을 퇴실 처리하고 호실을 청소 대상으로 되돌린다(D-031). */
	@PostMapping("/{id}/check-out")
	public String checkOut(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			stayService.checkOut(id);
			redirect.addFlashAttribute("flashSuccess", "체크아웃했습니다.");
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("flashError", "체크아웃할 수 없습니다: " + e.getMessage());
		}
		return "redirect:/admin/reservations/" + id;
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
