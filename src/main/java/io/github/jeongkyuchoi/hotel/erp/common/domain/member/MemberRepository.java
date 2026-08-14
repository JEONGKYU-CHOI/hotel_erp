package io.github.jeongkyuchoi.hotel.erp.common.domain.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 고객 회원 조회 (부킹엔진 JWT 인증, D-008). */
public interface MemberRepository extends JpaRepository<Member, Long> {

	/** 로그인용 조회. 활성 여부는 서비스에서 {@link Member#canLogin} 로 판정한다. */
	Optional<Member> findByTenantIdAndEmail(Long tenantId, String email);

	/** 회원가입 중복 이메일 선검사. 유니크 제약 {@code uk_member_email} 이 최후에 막는다. */
	boolean existsByTenantIdAndEmail(Long tenantId, String email);

	/** 수신거부 링크 토큰으로 회원을 찾는다(로그인 없이 옵트아웃). */
	Optional<Member> findByUnsubscribeToken(String unsubscribeToken);
}
