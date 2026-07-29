package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.RoomForm;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.OccupancyStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 호실 기준정보 관리. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

	private static final Long TENANT_ID = 1L;

	private final RoomRepository roomRepository;
	private final RoomTypeRepository roomTypeRepository;

	public List<Room> findAll() {
		return roomRepository.findAllForList(TENANT_ID);
	}

	public Room get(Long id) {
		return roomRepository.findByIdAndTenantId(id, TENANT_ID)
				.orElseThrow(() -> new NotFoundException("호실을 찾을 수 없습니다. id=" + id));
	}

	public boolean isRoomNoTaken(String roomNo) {
		return roomRepository.existsByTenantIdAndRoomNo(TENANT_ID, roomNo);
	}

	public boolean isRoomNoTakenByOther(String roomNo, Long id) {
		return roomRepository.existsByTenantIdAndRoomNoAndIdNot(TENANT_ID, roomNo, id);
	}

	@Transactional
	public Long create(RoomForm form) {
		Room room = Room.builder()
				.tenantId(TENANT_ID)
				.roomType(findRoomType(form.getRoomTypeId()))
				.roomNo(form.getRoomNo())
				.floor(form.getFloor())
				// 새 호실은 항상 비어 있는 상태로 시작한다. 화면에서 고르게 하지 않는다.
				.occupancyStatus(OccupancyStatus.VACANT)
				.cleanStatus(form.getCleanStatus())
				.active(form.isActive())
				.build();
		return roomRepository.save(room).getId();
	}

	@Transactional
	public void update(Long id, RoomForm form) {
		Room room = get(id);
		room.update(
				findRoomType(form.getRoomTypeId()),
				form.getRoomNo(),
				form.getFloor(),
				form.getCleanStatus());
		room.changeActive(form.isActive());
	}

	@Transactional
	public boolean toggleActive(Long id) {
		Room room = get(id);
		boolean next = !room.isActive();
		room.changeActive(next);
		return next;
	}

	/**
	 * 폼이 넘긴 객실타입 id 를 실제 엔티티로 바꾼다.
	 *
	 * <p>{@code getReference()} 로 프록시만 받아 오는 방법도 있지만 쓰지 않는다.
	 * 그러면 존재하지 않는 id 를 넘겨도 저장 시점까지 오류가 드러나지 않고,
	 * 그때는 FK 위반이라는 알아보기 힘든 예외로 나온다. 지금 조회해서 지금 실패시킨다.
	 */
	private RoomType findRoomType(Long roomTypeId) {
		return roomTypeRepository.findByIdAndTenantId(roomTypeId, TENANT_ID)
				.orElseThrow(() -> new NotFoundException(
						"객실타입을 찾을 수 없습니다. id=" + roomTypeId));
	}
}
