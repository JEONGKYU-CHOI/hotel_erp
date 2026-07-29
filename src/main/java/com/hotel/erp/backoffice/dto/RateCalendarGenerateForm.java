package com.hotel.erp.backoffice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 요금 캘린더 생성 폼.
 *
 * <p>주중/주말 금액을 나눠 받는다. 성수기·주말 요금 차등이 이 화면의 존재 이유다.
 * 주말 금액을 비우면 주중 금액을 그대로 쓴다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RateCalendarGenerateForm {

	/** 재고 생성과 같은 상한. 실수로 몇 년치를 만드는 것을 막는다. */
	public static final int MAX_DAYS = 400;

	@NotNull(message = "요금정책을 선택하세요.")
	private Long ratePlanId;

	@NotNull(message = "시작일을 입력하세요.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate fromDate;

	@NotNull(message = "종료일을 입력하세요.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate toDate;

	/** 주중(월~금) 요금. */
	@NotNull(message = "주중 요금을 입력하세요.")
	@DecimalMin(value = "0", message = "요금은 0 이상이어야 합니다.")
	@Digits(integer = 10, fraction = 2, message = "요금 형식이 올바르지 않습니다.")
	private BigDecimal weekdayAmount;

	/** 주말(토·일) 요금. 비우면 주중 요금을 쓴다. */
	@DecimalMin(value = "0", message = "요금은 0 이상이어야 합니다.")
	@Digits(integer = 10, fraction = 2, message = "요금 형식이 올바르지 않습니다.")
	private BigDecimal weekendAmount;

	/** 해당 날짜의 적용 요금. 토·일이면 주말가(없으면 주중가), 그 외엔 주중가. */
	public BigDecimal amountFor(LocalDate date) {
		DayOfWeek dow = date.getDayOfWeek();
		boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
		if (weekend && weekendAmount != null) {
			return weekendAmount;
		}
		return weekdayAmount;
	}

	@AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
	public boolean isDateRangeOrdered() {
		return fromDate == null || toDate == null || !toDate.isBefore(fromDate);
	}

	@AssertTrue(message = "한 번에 " + MAX_DAYS + "일까지만 생성할 수 있습니다.")
	public boolean isDateRangeWithinLimit() {
		if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
			return true; // 다른 검증이 잡는다. 여기서 중복해서 오류를 내지 않는다.
		}
		return ChronoUnit.DAYS.between(fromDate, toDate) + 1 <= MAX_DAYS;
	}
}
