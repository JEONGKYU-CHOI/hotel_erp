package io.github.jeongkyuchoi.hotel.erp.auth.web;

import io.github.jeongkyuchoi.hotel.erp.auth.dto.MeResponse;
import io.github.jeongkyuchoi.hotel.erp.auth.service.MemberAuthService;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.MyReservationSummary;
import io.github.jeongkyuchoi.hotel.erp.reservation.service.ReservationQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 로그인 회원 (D-009). JWT 인증이 필요한 첫 보호 경로다.
 *
 * <p>{@code @AuthenticationPrincipal} 로 주입되는 값은 {@code JwtAuthenticationFilter} 가
 * 인증 컨텍스트에 심은 <b>회원 id</b>다. 토큰이 없거나 무효면 이 경로에 닿기 전에
 * {@code RestAuthenticationEntryPoint} 가 401 로 막는다.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

	private final MemberAuthService memberAuthService;
	private final ReservationQueryService reservationQueryService;

	@GetMapping
	public MeResponse me(@AuthenticationPrincipal Long memberId) {
		return MeResponse.from(memberAuthService.getActiveMember(memberId));
	}

	/** 로그인 회원의 예약 목록(D-032). 인증된 회원 id 로만 조회한다 — 소유 증명 불필요. */
	@GetMapping("/reservations")
	public List<MyReservationSummary> myReservations(@AuthenticationPrincipal Long memberId) {
		return reservationQueryService.listForMember(memberId);
	}
}
