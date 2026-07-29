package com.hotel.erp.common.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 생성/수정 시각을 자동으로 채우는 기반 클래스.
 *
 * <p>{@code @MappedSuperclass} 는 이 클래스 자체는 테이블이 되지 않고
 * 상속받은 엔티티의 컬럼으로 합쳐진다는 뜻이다.
 *
 * <p>{@link AuditingEntityListener} 가 persist/update 시점에 값을 채운다.
 * 동작하려면 설정에 {@code @EnableJpaAuditing} 이 켜져 있어야 한다
 * (JpaAuditingConfig 참조).
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
