package io.github.jeongkyuchoi.hotel.erp.booking.web;

import java.util.Map;

/**
 * REST 오류 응답 본문(D-030).
 *
 * @param code    기계가 분기할 짧은 코드 (NOT_FOUND, NO_INVENTORY, VALIDATION …)
 * @param message 사람이 읽을 메시지
 * @param fields  필드별 검증 오류 (검증 실패에만 채워진다, 없으면 null)
 */
public record ApiError(String code, String message, Map<String, String> fields) {

	static ApiError of(String code, String message) {
		return new ApiError(code, message, null);
	}

	static ApiError validation(String message, Map<String, String> fields) {
		return new ApiError("VALIDATION", message, fields);
	}
}
