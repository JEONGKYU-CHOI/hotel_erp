package io.github.jeongkyuchoi.hotel.erp.booking.web;

import io.github.jeongkyuchoi.hotel.erp.common.exception.ConflictException;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotEnoughInventoryException;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.common.exception.PaymentException;
import io.github.jeongkyuchoi.hotel.erp.common.exception.UnauthorizedException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 부킹엔진 REST 예외 → HTTP 매핑(D-030).
 *
 * <p><b>{@code basePackages} 로 API 웹 패키지에만 건다.</b> 전역에 걸면 백오피스(HTML
 * 렌더링)의 예외까지 JSON 으로 바꿔 버린다. 백오피스는 폼 바인딩·플래시로 오류를 다루므로
 * (그쪽 컨트롤러 참조), 이 어드바이스는 booking·auth REST 컨트롤러에만 적용되게 범위를 좁힌다.
 *
 * <p>매핑: 없음 404 · 인증 실패 401 · 충돌(중복) 409 · 재고 부족 409(경합의 정상 결과) ·
 * 잘못된 요청 400.
 */
@Slf4j
@RestControllerAdvice(basePackages = {
		"io.github.jeongkyuchoi.hotel.erp.booking.web",
		"io.github.jeongkyuchoi.hotel.erp.auth.web",
		"io.github.jeongkyuchoi.hotel.erp.payment.web"})
public class ApiExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiError.of("NOT_FOUND", e.getMessage()));
	}

	/** 인증 실패(잘못된 자격증명). 로그인에서 이메일/비밀번호를 구분하지 않는다. */
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiError.of("UNAUTHORIZED", e.getMessage()));
	}

	/** 상태 충돌(중복 이메일 등). */
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of("CONFLICT", e.getMessage()));
	}

	/**
	 * 재고 부족. 오류가 아니라 <b>경합의 정상 결과</b>다 — 같은 방을 여럿이 노리면 누군가는
	 * 못 잡는다. 409 Conflict 로 돌려주고, 부킹엔진은 "방금 마감됐습니다"로 안내한다.
	 */
	@ExceptionHandler(NotEnoughInventoryException.class)
	public ResponseEntity<ApiError> handleNoInventory(NotEnoughInventoryException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of("NO_INVENTORY", e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException e) {
		return ResponseEntity.badRequest()
				.body(ApiError.of("BAD_REQUEST", e.getMessage()));
	}

	/**
	 * 결제 실패(D-034). 금액 위변조·카드 거절·응답 불일치 등. 토스/우리가 붙인 오류 코드를
	 * 그대로 실어 400 으로 돌려준다 — 프론트가 코드로 사용자 안내를 분기한다.
	 */
	@ExceptionHandler(PaymentException.class)
	public ResponseEntity<ApiError> handlePayment(PaymentException e) {
		return ResponseEntity.badRequest()
				.body(ApiError.of(e.getCode(), e.getMessage()));
	}

	/**
	 * 잘못된 상태 전이. 만료·취소·이미 확정된 예약에 결제가 도착한 경우 등(D-034). 상태가
	 * 더는 그 전이를 허용하지 않는 <b>충돌</b>이므로 409 로 돌려준다.
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiError> handleInvalidState(IllegalStateException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of("INVALID_STATE", e.getMessage()));
	}

	/** {@code @Valid} 바디 검증 실패. 필드별 메시지를 함께 돌려준다. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError fe : e.getBindingResult().getFieldErrors()) {
			fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
		}
		return ResponseEntity.badRequest()
				.body(ApiError.validation("입력값을 확인하세요.", fields));
	}
}
