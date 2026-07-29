package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 요금정책 등록/수정 폼.
 *
 * <p>금액 항목은 {@link BigDecimal} 로 받는다. {@code double} 로 받으면 입력 단계에서
 * 이미 오차가 섞이고, 그 값이 그대로 저장돼 정산에서 어긋난다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RatePlanForm {

	@NotNull(message = "객실타입을 선택하세요.")
	private Long roomTypeId;

	@NotBlank(message = "코드를 입력하세요.")
	@Size(max = 20, message = "코드는 20자 이내여야 합니다.")
	@Pattern(regexp = "^[A-Z0-9_]+$", message = "코드는 대문자·숫자·밑줄만 사용할 수 있습니다.")
	private String code;

	@NotBlank(message = "정책명을 입력하세요.")
	@Size(max = 100, message = "정책명은 100자 이내여야 합니다.")
	private String name;

	/**
	 * 기본 요금. 해당 일자에 요금 캘린더 행이 없을 때 쓰인다.
	 *
	 * <p>{@code @Digits} 로 자릿수를 제한한다. DB 컬럼이 {@code DECIMAL(12,2)} 라
	 * 넘치면 저장 시점에 잘리거나 예외가 난다. 화면에서 먼저 막는다.
	 */
	@NotNull(message = "기본 요금을 입력하세요.")
	@DecimalMin(value = "0", message = "요금은 0 이상이어야 합니다.")
	@Digits(integer = 10, fraction = 2, message = "요금 형식이 올바르지 않습니다.")
	private BigDecimal baseAmount;

	private boolean breakfastIncluded;

	/** 환불 불가 정책은 보통 더 싸다. 취소 정책 항목들과 함께 쓰인다. */
	private boolean refundable = true;

	@Min(value = 0, message = "취소 기한은 0일 이상이어야 합니다.")
	@Max(value = 365, message = "취소 기한이 너무 깁니다.")
	private short cancelDeadlineDays = 1;

	@NotNull(message = "위약금율을 입력하세요.")
	@DecimalMin(value = "0", message = "위약금율은 0 이상이어야 합니다.")
	@DecimalMax(value = "100", message = "위약금율은 100 이하여야 합니다.")
	@Digits(integer = 3, fraction = 2, message = "위약금율 형식이 올바르지 않습니다.")
	private BigDecimal penaltyRate = BigDecimal.ZERO;

	private boolean active = true;

	public static RatePlanForm from(RatePlan ratePlan) {
		RatePlanForm form = new RatePlanForm();
		form.roomTypeId = ratePlan.getRoomType().getId();
		form.code = ratePlan.getCode();
		form.name = ratePlan.getName();
		form.baseAmount = ratePlan.getBaseAmount();
		form.breakfastIncluded = ratePlan.isBreakfastIncluded();
		form.refundable = ratePlan.isRefundable();
		form.cancelDeadlineDays = ratePlan.getCancelDeadlineDays();
		form.penaltyRate = ratePlan.getPenaltyRate();
		form.active = ratePlan.isActive();
		return form;
	}
}
