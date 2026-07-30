package io.github.jeongkyuchoi.hotel.erp.folio.web;

import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse;
import io.github.jeongkyuchoi.hotel.erp.folio.service.FolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고객 청구서(폴리오) 조회 (D-038). 로그인 회원이 자기 예약의 청구서만 본다.
 *
 * <p>{@code /api/me/**} 아래라 JWT 인증이 필요하다 — 토큰이 없거나 무효면 이 경로에 닿기 전
 * {@code RestAuthenticationEntryPoint} 가 401 로 막는다. 소유(회원 일치)는 서비스가 한 번 더
 * 확인한다(D-032).
 */
@RestController
@RequestMapping("/api/me/reservations")
@RequiredArgsConstructor
public class FolioApiController {

	private final FolioService folioService;

	@GetMapping("/{reservationNo}/folio")
	public FolioResponse folio(
			@AuthenticationPrincipal Long memberId, @PathVariable String reservationNo) {
		return folioService.forMember(memberId, reservationNo);
	}
}
