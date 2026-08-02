package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.RoomTypeForm;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 객실타입 기준정보 관리.
 *
 * <p>클래스에 {@code @Transactional(readOnly = true)} 를 걸고, 쓰기 메서드에만
 * 다시 {@code @Transactional} 을 붙인다. 기본값을 읽기 전용으로 두면 실수로 변경이
 * 새어 나가지 않고, JPA 가 변경 감지(dirty checking)용 스냅샷을 만들지 않아 조회가 가볍다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomTypeService {

	/** 멀티테넌시는 백로그이므로 전부 1 고정이다(D-007). */
	private static final Long TENANT_ID = 1L;

	private final RoomTypeRepository roomTypeRepository;

	public List<RoomType> findAll() {
		return roomTypeRepository.findByTenantIdOrderByDisplayOrderAscIdAsc(TENANT_ID);
	}

	public RoomType get(Long id) {
		return roomTypeRepository.findByIdAndTenantId(id, TENANT_ID)
				.orElseThrow(() -> new NotFoundException("객실타입을 찾을 수 없습니다. id=" + id));
	}

	/** 등록 화면에서 코드 중복을 미리 알려주기 위한 검사. */
	public boolean isCodeTaken(String code) {
		return roomTypeRepository.existsByTenantIdAndCode(TENANT_ID, code);
	}

	@Transactional
	public Long create(RoomTypeForm form) {
		RoomType roomType = RoomType.builder()
				.tenantId(TENANT_ID)
				.code(form.getCode())
				.name(form.getName())
				.nameEn(form.getNameEn())
				.description(form.getDescription())
				.imageUrl(form.getImageUrl())
				.standardOccupancy(form.getStandardOccupancy())
				.maxOccupancy(form.getMaxOccupancy())
				.bedType(form.getBedType())
				.displayOrder(form.getDisplayOrder())
				.active(form.isActive())
				.build();
		return roomTypeRepository.save(roomType).getId();
	}

	/**
	 * 수정.
	 *
	 * <p>{@code save()} 를 부르지 않는다. 영속 상태인 엔티티의 값을 바꾸면 JPA 가
	 * 트랜잭션이 끝날 때 달라진 필드를 찾아 UPDATE 를 내보낸다(변경 감지).
	 * MyBatis 처럼 "고쳤으니 update 를 호출한다"가 아니라 "고치면 반영된다"이다.
	 */
	@Transactional
	public void update(Long id, RoomTypeForm form) {
		RoomType roomType = get(id);
		roomType.update(
				form.getName(),
				form.getNameEn(),
				form.getDescription(),
				form.getImageUrl(),
				form.getStandardOccupancy(),
				form.getMaxOccupancy(),
				form.getBedType(),
				form.getDisplayOrder());
		roomType.changeActive(form.isActive());
	}

	/** 판매 재개/중단 토글. 삭제 대신 쓴다(RoomType.changeActive 주석 참조). */
	@Transactional
	public boolean toggleActive(Long id) {
		RoomType roomType = get(id);
		boolean next = !roomType.isActive();
		roomType.changeActive(next);
		return next;
	}
}
