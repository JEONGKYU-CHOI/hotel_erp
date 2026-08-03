package io.github.jeongkyuchoi.hotel.erp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 요청 자체가 잘못됐을 때 던진다(잘못된 파일 형식·빈 값 등).
 *
 * <p>{@code @ResponseStatus} 는 전용 처리기가 없을 때의 폴백이다 — 400 으로 응답한다.
 * 화면(폼) 흐름에서는 컨트롤러가 이 예외를 잡아 필드 에러로 바꿔 사용자에게 돌려주기도 한다.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}
}
