package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.booking.dto.RatePlanSummary;
import io.github.jeongkyuchoi.hotel.erp.booking.dto.RoomTypeSummary;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부킹엔진 객실타입 카탈로그(D-030 보완). 고객 화면이 "무슨 타입이 팔리는가"를 물을 때 답한다.
 *
 * <p>판매 중단(비활성) 타입은 제외하고 노출 순서로 정렬한다 — 리포지토리의
 * {@code findByTenantIdAndActiveTrueOrderBy...} 가 그 규칙을 담고 있다.
 */
@Service
@RequiredArgsConstructor
public class RoomTypeCatalogService {

	private static final Long TENANT_ID = 1L;

	private final RoomTypeRepository roomTypeRepository;
	private final RatePlanRepository ratePlanRepository;

	@Transactional(readOnly = true)
	public List<RoomTypeSummary> listBookable() {
		return roomTypeRepository
				.findByTenantIdAndActiveTrueOrderByDisplayOrderAscIdAsc(TENANT_ID)
				.stream()
				.map(RoomTypeSummary::from)
				.toList();
	}

	/** 한 객실타입의 판매 중 요금정책. HOLD 요청에 실을 ratePlanId 를 고르는 화면이 소비한다. */
	@Transactional(readOnly = true)
	public List<RatePlanSummary> listRatePlans(Long roomTypeId) {
		return ratePlanRepository.findBookableByRoomType(TENANT_ID, roomTypeId)
				.stream()
				.map(RatePlanSummary::from)
				.toList();
	}
}
