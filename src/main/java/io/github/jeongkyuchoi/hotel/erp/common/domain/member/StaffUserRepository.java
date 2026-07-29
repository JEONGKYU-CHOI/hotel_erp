package io.github.jeongkyuchoi.hotel.erp.common.domain.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 백오피스 직원 계정 조회. */
public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

	/**
	 * 로그인용 조회.
	 *
	 * <p>{@code active = false}(퇴사 등)인 계정은 아예 찾지 못하게 한다. 조회한 뒤
	 * 서비스에서 걸러도 되지만, 쿼리 단계에서 막으면 그 검사를 빠뜨릴 여지가 없다.
	 */
	Optional<StaffUser> findByTenantIdAndUsernameAndActiveTrue(Long tenantId, String username);

	boolean existsByTenantIdAndUsername(Long tenantId, String username);
}
