package io.github.jeongkyuchoi.hotel.erp.common.domain.member;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 백오피스 직원 계정 (Thymeleaf, 세션 쿠키 인증 — D-001).
 *
 * <p>고객({@link Member})과 테이블을 분리한 이유는 D-014 에 있다.
 * 요약하면 인증 경로가 애초에 다르고, 한 테이블이면 고객 레코드에 ADMIN 권한을
 * 넣을 수 있는 구조가 되기 때문이다.
 */
@Entity
@Table(name = "staff_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffUser extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "username", nullable = false, length = 50)
	private String username;

	/** BCrypt 해시. {@link Member#getPasswordHash()} 와 같은 주의사항이 적용된다. */
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private StaffRole role;

	/** 퇴사 처리는 삭제가 아니라 비활성화다. 이 계정이 남긴 감사 기록이 있다. */
	@Column(name = "active", nullable = false)
	private boolean active;

	@Builder
	private StaffUser(Long tenantId, String username, String passwordHash, String name,
			StaffRole role, boolean active) {
		this.tenantId = tenantId;
		this.username = username;
		this.passwordHash = passwordHash;
		this.name = name;
		this.role = role;
		this.active = active;
	}
}
