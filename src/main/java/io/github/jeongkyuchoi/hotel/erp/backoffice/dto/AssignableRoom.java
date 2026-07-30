package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;

/**
 * 체크인 배정 후보 호실(D-031). 상세 화면의 호실 선택 드롭다운에 쓴다.
 *
 * @param floor 층. 없을 수 있어 Short.
 */
public record AssignableRoom(Long id, String roomNo, Short floor) {

	public static AssignableRoom from(Room room) {
		return new AssignableRoom(room.getId(), room.getRoomNo(), room.getFloor());
	}
}
