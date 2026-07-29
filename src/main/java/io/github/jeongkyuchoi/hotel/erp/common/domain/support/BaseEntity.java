package io.github.jeongkyuchoi.hotel.erp.common.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * 생성/수정 시각 + 생성자/수정자를 함께 기록하는 기반 클래스.
 *
 * <p>감사 추적(audit trail)을 위한 것이다. 브리프에서는 감사로그를 4순위 폐기 대상으로
 * 두었으나 유지하기로 했다(D-006 논의): 구현 비용이 2시간 수준인데,
 * "폴리오는 append-only, 정정은 역분개" 라는 설계 원칙(브리프 5번)과 직결되기 때문이다.
 * 감사 추적을 원칙으로 내세우고 감사 기록이 없으면 그 주장이 비어 보인다.
 *
 * <p>누가 바꿨는지가 의미 있는 엔티티(예약, 폴리오, 기준정보)는 이 클래스를,
 * 시각만 필요한 엔티티는 {@link BaseTimeEntity} 를 상속한다.
 *
 * <p>값은 {@code AuditorAware} 구현체가 공급한다(JpaAuditingConfig 참조).
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

	@CreatedBy
	@Column(name = "created_by", length = 50, updatable = false)
	private String createdBy;

	@LastModifiedBy
	@Column(name = "updated_by", length = 50)
	private String updatedBy;
}
