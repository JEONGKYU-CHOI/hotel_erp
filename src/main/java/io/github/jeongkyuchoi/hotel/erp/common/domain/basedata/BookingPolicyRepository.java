package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 예약 정책 조회. 테넌트당 한 행이다. */
public interface BookingPolicyRepository extends JpaRepository<BookingPolicy, Long> {

	Optional<BookingPolicy> findByTenantId(Long tenantId);
}
