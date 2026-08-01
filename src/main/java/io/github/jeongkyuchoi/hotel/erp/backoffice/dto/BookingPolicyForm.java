package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.BookingPolicy;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 예약 정책 편집 폼(PMS).
 *
 * <p>{@code <input type="time">} 가 보내는 "HH:mm" 을 {@link LocalTime} 으로 바인딩하기 위해
 * {@code @DateTimeFormat(iso = TIME)} 를 건다.
 */
@Getter
@Setter
public class BookingPolicyForm {

	@NotNull(message = "당일 마감 시각은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
	private LocalTime sameDayCutoffTime;

	public static BookingPolicyForm from(BookingPolicy policy) {
		BookingPolicyForm form = new BookingPolicyForm();
		form.sameDayCutoffTime = policy.getSameDayCutoffTime();
		return form;
	}
}
