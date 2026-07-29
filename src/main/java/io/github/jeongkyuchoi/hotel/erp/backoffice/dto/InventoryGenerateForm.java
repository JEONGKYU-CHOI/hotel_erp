package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** 재고 생성 폼. */
@Getter
@Setter
@NoArgsConstructor
public class InventoryGenerateForm {

	/** 한 번에 생성할 수 있는 최대 일수. 실수로 10년치를 만드는 것을 막는다. */
	public static final int MAX_DAYS = 400;

	@NotNull(message = "객실타입을 선택하세요.")
	private Long roomTypeId;

	/**
	 * {@code @DateTimeFormat} 이 없으면 HTML {@code <input type="date">} 가 보내는
	 * "2026-07-29" 문자열을 {@link LocalDate} 로 바꾸지 못해 바인딩이 실패한다.
	 */
	@NotNull(message = "시작일을 입력하세요.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate fromDate;

	@NotNull(message = "종료일을 입력하세요.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate toDate;

	/**
	 * 판매 총량. 비워 두면 해당 객실타입의 <b>사용 중인 호실 수</b>를 쓴다.
	 *
	 * <p>직접 넣을 수 있게 열어 둔 이유 — 호실 일부를 장기 수리로 빼거나,
	 * 반대로 오버부킹을 감수하고 몇 개 더 파는 것이 실무에서 실제로 일어난다.
	 */
	@Min(value = 0, message = "판매 총량은 0 이상이어야 합니다.")
	@Max(value = 9999, message = "판매 총량이 너무 큽니다.")
	private Integer totalQty;

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
