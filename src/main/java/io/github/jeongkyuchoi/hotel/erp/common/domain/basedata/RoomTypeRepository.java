package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 객실타입 조회. */
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

	/** 백오피스 목록. 노출 순서 → id 순으로 안정 정렬한다. */
	List<RoomType> findByTenantIdOrderByDisplayOrderAscIdAsc(Long tenantId);

	/** 부킹엔진 노출용. 판매 중단(비활성) 타입은 제외한다. */
	List<RoomType> findByTenantIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long tenantId);

	/**
	 * 단건 조회에 {@code tenantId} 를 함께 건다.
	 *
	 * <p>{@code findById(id)} 만 쓰면 URL 의 id 를 바꾸는 것만으로 다른 테넌트의
	 * 데이터가 열린다. 지금은 테넌트가 하나뿐이라 차이가 없지만(D-007), 조회 습관을
	 * 처음부터 이렇게 들여야 나중에 멀티테넌시를 켤 때 빠뜨린 곳을 찾아다니지 않는다.
	 */
	Optional<RoomType> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByTenantIdAndCode(Long tenantId, String code);

	/** 수정 시 코드 중복 검사. 자기 자신은 제외한다. */
	boolean existsByTenantIdAndCodeAndIdNot(Long tenantId, String code, Long id);
}
