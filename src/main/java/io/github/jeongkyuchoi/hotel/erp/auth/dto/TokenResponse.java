package io.github.jeongkyuchoi.hotel.erp.auth.dto;

/**
 * 로그인 성공 응답 — Access Token(D-010, 리프레시 없음).
 *
 * @param tokenType 항상 "Bearer". 클라이언트는 {@code Authorization: Bearer <token>} 로 보낸다.
 * @param expiresInSeconds 토큰 수명(초). 만료되면 재로그인한다.
 */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

	public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
		return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
	}
}
