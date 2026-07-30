package io.github.jeongkyuchoi.hotel.erp.booking.dto;

import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 부킹엔진 HOLD 생성 요청(D-030).
 *
 * <p>웹 계층의 입력 검증을 여기서 애노테이션으로 건다. 검증을 통과한 값만 서비스 입력
 * ({@link ReservationHoldCommand})으로 넘긴다 — 서비스가 REST DTO 에 직접 의존하지 않게
 * 분리한다(홀드 커맨드 주석과 같은 규율).
 *
 * <p>비회원 예약이 기본 경로이므로 {@code memberId} 는 없다. 로그인 회원 예약은 인증(D-008)이
 * 붙을 때 다룬다.
 */
public record HoldRequest(
		@NotBlank(message = "예약자 이름은 필수입니다.")
		String guestName,

		@NotBlank(message = "연락처는 필수입니다.")
		String guestPhone,

		@Email(message = "이메일 형식이 올바르지 않습니다.")
		String guestEmail,

		@NotNull(message = "객실타입을 선택하세요.")
		Long roomTypeId,

		@NotNull(message = "요금정책을 선택하세요.")
		Long ratePlanId,

		@NotNull(message = "체크인 날짜는 필수입니다.")
		@Future(message = "체크인 날짜는 오늘 이후여야 합니다.")
		LocalDate checkInDate,

		@NotNull(message = "체크아웃 날짜는 필수입니다.")
		@Future(message = "체크아웃 날짜는 오늘 이후여야 합니다.")
		LocalDate checkOutDate,

		@Min(value = 1, message = "성인은 1명 이상이어야 합니다.")
		int adults,

		@Min(value = 0, message = "아동 수가 올바르지 않습니다.")
		int children,

		@NotBlank(message = "멱등키는 필수입니다.")
		String idempotencyKey) {

	/**
	 * 서비스 입력으로 변환한다. 로그인 회원이면 인증 컨텍스트의 {@code memberId} 를 싣고,
	 * 비회원이면 {@code null} 이다(D-032). 회원 여부는 클라이언트가 보내는 값이 아니라
	 * 서버가 토큰에서 뽑은 값이라, 남의 회원 id 로 예약을 붙이는 위조가 성립하지 않는다.
	 */
	public ReservationHoldCommand toCommand(Long memberId) {
		return new ReservationHoldCommand(
				memberId, guestName, guestPhone, guestEmail,
				roomTypeId, ratePlanId, checkInDate, checkOutDate,
				adults, children, idempotencyKey);
	}
}
