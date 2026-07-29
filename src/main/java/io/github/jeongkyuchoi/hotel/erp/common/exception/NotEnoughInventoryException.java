package io.github.jeongkyuchoi.hotel.erp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 요청한 재고가 남아 있지 않을 때 던진다.
 *
 * <p>이 프로젝트의 핵심 방어선이다. 예약 확정 트랜잭션에서 {@code FOR UPDATE} 로
 * 잠근 재고 행의 가용량이 부족하면 던져지고, 트랜잭션이 롤백되어 오버부킹을 막는다.
 * DB 의 {@code CHECK (sold_qty + held_qty <= total_qty)} 가 최후의 방어선이지만,
 * 거기까지 가면 알아볼 수 없는 예외가 되므로 도메인에서 먼저 걸러 사유를 남긴다.
 *
 * <p>{@code 409 CONFLICT} 인 이유 — 요청 자체는 올바르다. 다른 예약과 <b>경합</b>해
 * 자원이 부족해진 것이라 상태 충돌(conflict)이 정확한 의미다. 400(요청 오류)이 아니다.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class NotEnoughInventoryException extends RuntimeException {

	public NotEnoughInventoryException(String message) {
		super(message);
	}
}
