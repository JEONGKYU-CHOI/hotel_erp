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
 * 고객 회원 (부킹엔진, JWT 인증 — D-008).
 *
 * <p>비회원 예약이 기본이므로 회원은 선택 사항이다. 로그인 없이도 예약번호 + 전화번호로
 * 예약을 조회할 수 있고, 회원은 그 위에 얹히는 편의 기능이다(D-009).
 *
 * <p>직원 계정은 {@link StaffUser} 로 분리돼 있다(D-014).
 */
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	/**
	 * BCrypt 해시. 평문 비밀번호는 어떤 경우에도 이 필드에 들어오면 안 된다.
	 *
	 * <p>getter 가 자동 생성되므로 로그나 DTO 에 실수로 실릴 수 있다.
	 * 응답 DTO 를 만들 때 이 필드가 포함되지 않았는지 반드시 확인할 것.
	 */
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	/** 비회원 예약 조회 키와 같은 형식으로 보관한다. */
	@Column(name = "phone", nullable = false, length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private MemberStatus status;

	@Builder
	private Member(Long tenantId, String email, String passwordHash, String name,
			String phone, MemberStatus status) {
		this.tenantId = tenantId;
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.phone = phone;
		this.status = status;
	}

	/** 로그인 가능한 상태인가. */
	public boolean canLogin() {
		return status == MemberStatus.ACTIVE;
	}
}
