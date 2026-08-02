package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.RatePlanForm;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 요금정책 기준정보 관리. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatePlanService {

	private static final Long TENANT_ID = 1L;

	private final RatePlanRepository ratePlanRepository;
	private final RoomTypeRepository roomTypeRepository;

	public List<RatePlan> findAll() {
		return ratePlanRepository.findAllForList(TENANT_ID);
	}

	public RatePlan get(Long id) {
		return ratePlanRepository.findByIdAndTenantId(id, TENANT_ID)
				.orElseThrow(() -> new NotFoundException("요금정책을 찾을 수 없습니다. id=" + id));
	}

	public boolean isCodeTaken(String code) {
		return ratePlanRepository.existsByTenantIdAndCode(TENANT_ID, code);
	}

	@Transactional
	public Long create(RatePlanForm form) {
		RatePlan ratePlan = RatePlan.builder()
				.tenantId(TENANT_ID)
				.roomType(findRoomType(form.getRoomTypeId()))
				.code(form.getCode())
				.name(form.getName())
				.nameEn(form.getNameEn())
				.baseAmount(form.getBaseAmount())
				.breakfastIncluded(form.isBreakfastIncluded())
				.refundable(form.isRefundable())
				.cancelDeadlineDays(form.getCancelDeadlineDays())
				.penaltyRate(form.getPenaltyRate())
				.active(form.isActive())
				.build();
		return ratePlanRepository.save(ratePlan).getId();
	}

	@Transactional
	public void update(Long id, RatePlanForm form) {
		RatePlan ratePlan = get(id);
		ratePlan.update(
				findRoomType(form.getRoomTypeId()),
				form.getName(),
				form.getNameEn(),
				form.getBaseAmount(),
				form.isBreakfastIncluded(),
				form.isRefundable(),
				form.getCancelDeadlineDays(),
				form.getPenaltyRate());
		ratePlan.changeActive(form.isActive());
	}

	@Transactional
	public boolean toggleActive(Long id) {
		RatePlan ratePlan = get(id);
		boolean next = !ratePlan.isActive();
		ratePlan.changeActive(next);
		return next;
	}

	private RoomType findRoomType(Long roomTypeId) {
		return roomTypeRepository.findByIdAndTenantId(roomTypeId, TENANT_ID)
				.orElseThrow(() -> new NotFoundException(
						"객실타입을 찾을 수 없습니다. id=" + roomTypeId));
	}
}
