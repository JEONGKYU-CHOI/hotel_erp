package io.github.jeongkyuchoi.hotel.erp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 인증 실패(잘못된 자격증명 등)일 때 던진다.
 *
 * <p>로그인에서 "이메일 없음"과 "비밀번호 틀림"을 구분하지 않고 이 하나로 던진다 —
 * 어느 쪽인지 흘리면 가입 여부를 확인해 주는 오라클이 된다.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
