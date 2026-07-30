package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 하우스키핑 — 청소 상태 전이 (D-040). 체크아웃이 남긴 DIRTY 호실을 청소 흐름으로 흘린다:
 * DIRTY(청소필요) → IN_PROGRESS(청소중) → CLEAN(청소완료).
 *
 * <p><b>전이는 호실 행을 잠그고 한다.</b> 두 직원이 같은 방을 동시에 잡거나, 청소 완료와
 * 체크인 배정이 겹칠 수 있으므로 {@code FOR UPDATE} 로 직렬화한다(D-031 락 규율의 연장).
 * 상태 가드는 도메인({@link Room#startCleaning}·{@link Room#finishCleaning})이 못 박는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HousekeepingService {

	private static final Long TENANT_ID = 1L;

	private final RoomRepository roomRepository;

	/** 청소 대상(청소필요·청소중) 호실 현황. 읽기 전용. */
	@Transactional(readOnly = true)
	public List<Room> board() {
		return roomRepository.findForHousekeeping(TENANT_ID);
	}

	/** 청소 시작 — DIRTY → IN_PROGRESS. */
	@Transactional
	public void startCleaning(Long roomId) {
		room(roomId).startCleaning();
	}

	/** 청소 완료 — IN_PROGRESS → CLEAN. 완료 호실은 다시 배정 대상이 된다. */
	@Transactional
	public void finishCleaning(Long roomId) {
		room(roomId).finishCleaning();
		log.info("청소 완료 — roomId={}", roomId);
	}

	private Room room(Long roomId) {
		return roomRepository.findByIdForUpdate(roomId)
				.orElseThrow(() -> new NotFoundException("호실을 찾을 수 없습니다. id=" + roomId));
	}
}
