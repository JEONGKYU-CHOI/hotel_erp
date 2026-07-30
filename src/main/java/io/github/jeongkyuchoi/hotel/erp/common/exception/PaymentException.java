package io.github.jeongkyuchoi.hotel.erp.common.exception;

import lombok.Getter;

/**
 * 결제 처리 실패 (D-034). 금액 위변조 거부, 토스 승인 거절(카드 한도 등), 응답 위조 등을 담는다.
 *
 * <p>{@code code} 를 함께 실어 나른다 — 토스가 준 오류 코드({@code ALREADY_PROCESSED_PAYMENT}
 * 등)나 우리가 붙인 코드({@code AMOUNT_MISMATCH})를 API 응답에 그대로 노출해, 프론트가
 * 사용자 안내를 분기할 수 있게 한다. HTTP 매핑은 {@code ApiExceptionHandler} 에서 한다.
 */
@Getter
public class PaymentException extends RuntimeException {

	private final String code;

	public PaymentException(String code, String message) {
		super(message);
		this.code = code;
	}
}
