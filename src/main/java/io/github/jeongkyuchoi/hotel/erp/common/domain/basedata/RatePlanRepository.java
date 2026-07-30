package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 요금정책 조회. */
public interface RatePlanRepository extends JpaRepository<RatePlan, Long> {

	/** 백오피스 목록. 객실타입을 함께 가져와 N+1 을 피한다. */
	@Query("""
			select rp from RatePlan rp
			join fetch rp.roomType rt
			where rp.tenantId = :tenantId
			order by rt.displayOrder asc, rt.id asc, rp.code asc
			""")
	List<RatePlan> findAllForList(Long tenantId);

	Optional<RatePlan> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByTenantIdAndCode(Long tenantId, String code);

	/**
	 * 부킹엔진 노출용. 한 객실타입의 판매 중(활성) 요금정책을 저렴한 순으로.
	 * 판매 중단 정책은 제외한다 — room-types 노출 규칙과 같은 결.
	 */
	@Query("""
			select rp from RatePlan rp
			where rp.tenantId = :tenantId
			  and rp.roomType.id = :roomTypeId
			  and rp.active = true
			order by rp.baseAmount asc, rp.id asc
			""")
	List<RatePlan> findBookableByRoomType(Long tenantId, Long roomTypeId);
}
