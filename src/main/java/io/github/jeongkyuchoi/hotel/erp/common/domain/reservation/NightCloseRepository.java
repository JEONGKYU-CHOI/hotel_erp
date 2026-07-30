package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 야간마감 이력 조회.
 *
 * <p>마감 서비스는 게시 전에 {@link #findByTenantIdAndBusinessDate} 로 이미 마감된
 * 영업일인지 먼저 확인한다(선조회 멱등). 유니크 제약 {@code uk_night_close} 가 최후에
 * 막지만, 예외를 던지기 전에 조회로 걸러 내는 편이 재실행 API 로서 깔끔하다(D-026).
 */
public interface NightCloseRepository extends JpaRepository<NightClose, Long> {

	Optional<NightClose> findByTenantIdAndBusinessDate(Long tenantId, LocalDate businessDate);
}
