package io.github.jeongkyuchoi.hotel.erp.housekeeping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.backoffice.service.HousekeepingService;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.CleanStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.OccupancyStatus;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 하우스키핑 청소 흐름 테스트 (D-040).
 *
 * <p>증명: ① DIRTY → IN_PROGRESS → CLEAN 3단계 전이, ② 청소 완료 호실은 다시 배정 대상,
 * ③ 상태 가드(청소필요 아닌데 시작·청소중 아닌데 완료는 거부), ④ 현황판은 청소필요·청소중만.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class HousekeepingTest {

	@Autowired private HousekeepingService housekeepingService;
	@Autowired private RoomRepository roomRepository;
	@Autowired private RoomTypeRepository roomTypeRepository;

	private Long roomTypeId;

	@BeforeEach
	void seed() {
		RoomType roomType = roomTypeRepository.save(RoomType.builder()
				.tenantId(1L).code("HK").name("청소 테스트 타입")
				.standardOccupancy(2).maxOccupancy(2).displayOrder(1).active(true)
				.build());
		roomTypeId = roomType.getId();
	}

	@AfterEach
	void cleanup() {
		roomRepository.deleteAll();
		roomTypeRepository.deleteAll();
	}

	private Room saveRoom(String no, CleanStatus clean) {
		return roomRepository.save(Room.builder()
				.tenantId(1L).roomType(roomTypeRepository.findById(roomTypeId).orElseThrow())
				.roomNo(no).floor((short) 3)
				.occupancyStatus(OccupancyStatus.VACANT).cleanStatus(clean).active(true)
				.build());
	}

	private CleanStatus cleanOf(Long roomId) {
		return roomRepository.findById(roomId).orElseThrow().getCleanStatus();
	}

	@Test
	@DisplayName("DIRTY → 청소시작 → IN_PROGRESS → 청소완료 → CLEAN, 완료 후 배정 대상")
	void fullCleaningFlow() {
		Long id = saveRoom("301", CleanStatus.DIRTY).getId();

		housekeepingService.startCleaning(id);
		assertThat(cleanOf(id)).isEqualTo(CleanStatus.IN_PROGRESS);

		housekeepingService.finishCleaning(id);
		assertThat(cleanOf(id)).isEqualTo(CleanStatus.CLEAN);
		assertThat(roomRepository.findById(id).orElseThrow().isAssignable())
				.as("청소완료·공실이면 다시 배정 가능").isTrue();
	}

	@Test
	@DisplayName("청소필요 아닌 호실은 청소를 시작할 수 없다")
	void cannotStart_whenNotDirty() {
		Long id = saveRoom("302", CleanStatus.CLEAN).getId();
		assertThatThrownBy(() -> housekeepingService.startCleaning(id))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("청소중 아닌 호실은 청소를 완료할 수 없다")
	void cannotFinish_whenNotInProgress() {
		Long id = saveRoom("303", CleanStatus.DIRTY).getId();
		assertThatThrownBy(() -> housekeepingService.finishCleaning(id))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("현황판은 청소필요·청소중만 — 청소완료·사용불가는 빠진다")
	void board_showsOnlyActionable() {
		saveRoom("401", CleanStatus.DIRTY);
		saveRoom("402", CleanStatus.IN_PROGRESS);
		saveRoom("403", CleanStatus.CLEAN);
		saveRoom("404", CleanStatus.OUT_OF_ORDER);

		var board = housekeepingService.board();

		assertThat(board).extracting(Room::getRoomNo)
				.containsExactlyInAnyOrder("401", "402");
	}
}
