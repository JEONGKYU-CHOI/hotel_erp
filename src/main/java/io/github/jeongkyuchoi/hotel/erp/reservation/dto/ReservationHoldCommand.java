package io.github.jeongkyuchoi.hotel.erp.reservation.dto;

import java.time.LocalDate;

/**
 * 예약 임시점유(HOLD) 생성 요청.
 *
 * <p>서비스 계층의 입력이다. 웹 계층(폼/REST DTO)의 검증을 통과한 값이 여기로 들어온다.
 * 서비스가 웹 프레임워크의 폼 객체에 직접 의존하지 않게 분리한다.
 *
 * @param memberId       로그인 회원이면 그 id, 비회원이면 null (비회원이 기본 경로)
 * @param idempotencyKey 더블클릭·재시도로 인한 중복 생성을 막는 키. 같은 키로 두 번
 *                       요청하면 이미 만든 예약을 그대로 돌려준다.
 */
public record ReservationHoldCommand(
		Long memberId,
		String guestName,
		String guestPhone,
		String guestEmail,
		Long roomTypeId,
		Long ratePlanId,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		int adults,
		int children,
		String idempotencyKey) {

	/** 숙박일수. 체크아웃 당일은 숙박에 포함되지 않는다. */
	public int nights() {
		return (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
	}
}
