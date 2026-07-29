package com.hotel.erp.backoffice.dto;

import com.hotel.erp.common.domain.basedata.CleanStatus;
import com.hotel.erp.common.domain.basedata.Room;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 호실 등록/수정 폼.
 *
 * <p>점유 상태({@code occupancyStatus})는 이 폼에 없다. 예약과 체크인이 바꾸는 값이라
 * 사람이 화면에서 고르면 안 된다({@link Room#update} 주석 참조).
 */
@Getter
@Setter
@NoArgsConstructor
public class RoomForm {

	@NotNull(message = "객실타입을 선택하세요.")
	private Long roomTypeId;

	@NotBlank(message = "호실 번호를 입력하세요.")
	@Size(max = 10, message = "호실 번호는 10자 이내여야 합니다.")
	private String roomNo;

	/**
	 * 층. 값이 없을 수 있어 {@code Short} 다.
	 *
	 * <p>기본형 {@code short} 로 두면 입력을 비웠을 때 0 으로 채워져
	 * "0층"이라는 없는 값이 저장된다. null 이 의미를 갖는 항목은 래퍼 타입을 쓴다.
	 */
	@Min(value = -10, message = "층 값이 올바르지 않습니다.")
	@Max(value = 200, message = "층 값이 올바르지 않습니다.")
	private Short floor;

	@NotNull(message = "청결 상태를 선택하세요.")
	private CleanStatus cleanStatus = CleanStatus.CLEAN;

	private boolean active = true;

	public static RoomForm from(Room room) {
		RoomForm form = new RoomForm();
		form.roomTypeId = room.getRoomType().getId();
		form.roomNo = room.getRoomNo();
		form.floor = room.getFloor();
		form.cleanStatus = room.getCleanStatus();
		form.active = room.isActive();
		return form;
	}
}
