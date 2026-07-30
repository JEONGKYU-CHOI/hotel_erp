package io.github.jeongkyuchoi.hotel.erp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 자원 상태 충돌(중복 등)일 때 던진다. 예: 이미 가입된 이메일로 회원가입.
 *
 * <p>{@code @ResponseStatus} 는 전용 처리기가 없을 때의 폴백이다. {@code /api} 는
 * {@code ApiExceptionHandler} 가 JSON 본문으로 매핑한다.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
