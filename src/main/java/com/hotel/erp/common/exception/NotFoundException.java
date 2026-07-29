package com.hotel.erp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 요청한 자원이 없을 때 던진다.
 *
 * <p>{@code @ResponseStatus} 를 붙여 두면 별도의 예외 처리기 없이도 500 이 아니라
 * 404 로 응답한다. 없는 것을 달라고 한 것은 서버 잘못이 아니다.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}
