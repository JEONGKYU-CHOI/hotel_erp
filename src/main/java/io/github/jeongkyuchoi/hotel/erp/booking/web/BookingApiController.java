package io.github.jeongkyuchoi.hotel.erp.booking.web;

import io.github.jeongkyuchoi.hotel.erp.booking.dto.AvailabilityResponse;
import io.github.jeongkyuchoi.hotel.erp.booking.dto.HoldRequest;
import io.github.jeongkyuchoi.hotel.erp.booking.dto.HoldResponse;
import io.github.jeongkyuchoi.hotel.erp.booking.dto.RatePlanSummary;
import io.github.jeongkyuchoi.hotel.erp.booking.dto.RoomTypeSummary;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationDetail;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.AvailabilityService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationQueryService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationService;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.RoomTypeCatalogService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부킹엔진 REST — 고객 대면 예약 흐름(D-030). React 부킹엔진의 백엔드다.
 *
 * <p><b>흐름</b>: 가용 조회({@code GET /api/availability}) → HOLD 생성
 * ({@code POST /api/reservations}) → 예약 조회({@code GET /api/reservations/{no}}).
 *
 * <p><b>확정(HOLD → CONFIRMED)은 이 컨트롤러에 없다.</b> 확정의 트리거는 결제 성공이다
 * (D-010, D-023). 결제 검증 없이 확정을 여는 REST 엔드포인트는 "돈 안 내고 확정"의 구멍이
 * 되므로, 확정은 결제 모듈의 웹훅이 붙을 때 그쪽에서 노출한다.
 *
 * <p><b>보안</b> — 이 체인은 아직 {@code permitAll} 이다(JWT 미구현, D-008). 그러므로
 * <b>JWT 전에는 외부에 노출하면 안 된다</b>. 조회는 예약번호+전화로 소유를 확인하고(D-028),
 * 생성은 멱등키·재고 락으로 보호되지만, 인증 자체는 아직 없다.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingApiController {

	private final AvailabilityService availabilityService;
	private final ReservationService reservationService;
	private final ReservationQueryService reservationQueryService;
	private final RoomTypeCatalogService roomTypeCatalogService;

	/** 판매 중인 객실타입 목록. 고객이 타입을 고르는 첫 화면이 소비한다. */
	@GetMapping("/room-types")
	public List<RoomTypeSummary> roomTypes() {
		return roomTypeCatalogService.listBookable();
	}

	/** 한 객실타입의 판매 중 요금정책. HOLD 직전 요금/조건을 고르는 화면이 소비한다. */
	@GetMapping("/room-types/{roomTypeId}/rate-plans")
	public List<RatePlanSummary> ratePlans(@PathVariable Long roomTypeId) {
		return roomTypeCatalogService.listRatePlans(roomTypeId);
	}

	/** 날짜 범위의 가용 재고. 락 없는 표시 경로다(D-018). */
	@GetMapping("/availability")
	public AvailabilityResponse availability(
			@RequestParam Long roomTypeId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
		return availabilityService.check(roomTypeId, checkIn, checkOut);
	}

	/**
	 * HOLD 생성. 오버부킹은 재고 락으로 막힌다(D-023). 멱등키로 중복 생성을 막는다.
	 *
	 * <p>로그인 회원이면 인증 컨텍스트의 회원 id 가 예약에 연결된다(D-032). 토큰이 없으면
	 * {@code memberId} 는 null 이라 비회원 예약이 된다 — 비회원 경로는 그대로 열려 있다(D-008).
	 */
	@PostMapping("/reservations")
	@ResponseStatus(HttpStatus.CREATED)
	public HoldResponse hold(@Valid @RequestBody HoldRequest request,
			@AuthenticationPrincipal Long memberId) {
		return HoldResponse.from(reservationService.hold(request.toCommand(memberId)));
	}

	/** 예약 조회 (예약번호 + 전화, 비회원 경로, D-028). */
	@GetMapping("/reservations/{reservationNo}")
	public ReservationDetail lookup(
			@PathVariable String reservationNo,
			@RequestParam String phone) {
		return reservationQueryService.findForGuest(reservationNo, phone);
	}
}
