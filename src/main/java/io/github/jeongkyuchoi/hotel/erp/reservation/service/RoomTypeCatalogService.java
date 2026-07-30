package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.booking.dto.RoomTypeSummary;
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

	@Transactional(readOnly = true)
	public List<RoomTypeSummary> listBookable() {
		return roomTypeRepository
				.findByTenantIdAndActiveTrueOrderByDisplayOrderAscIdAsc(TENANT_ID)
				.stream()
				.map(RoomTypeSummary::from)
				.toList();
	}
}
