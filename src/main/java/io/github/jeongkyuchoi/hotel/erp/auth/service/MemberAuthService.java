package io.github.jeongkyuchoi.hotel.erp.auth.service;

import io.github.jeongkyuchoi.hotel.erp.auth.dto.LoginRequest;
import io.github.jeongkyuchoi.hotel.erp.auth.dto.SignupRequest;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Member;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberStatus;
import io.github.jeongkyuchoi.hotel.erp.common.exception.ConflictException;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 가입·인증 (D-009). 비밀번호는 BCrypt 로 해싱해 저장한다(SecurityConfig 의 인코더).
 *
 * <p>직원 계정({@code StaffUser}, 세션 인증)과 경로가 다르다(D-014). 이 서비스는 고객 회원의
 * JWT 인증 경로만 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAuthService {

	private static final Long TENANT_ID = 1L;

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 회원가입. 비밀번호를 즉시 해싱해 저장한다 — 평문은 이 메서드 밖으로 나가지 않는다.
	 *
	 * @throws ConflictException 이미 가입된 이메일이면(유니크 제약이 최후에도 막는다).
	 */
	@Transactional
	public Member signup(SignupRequest request) {
		if (memberRepository.existsByTenantIdAndEmail(TENANT_ID, request.email())) {
			throw new ConflictException("이미 가입된 이메일입니다.");
		}
		Member member = Member.builder()
				.tenantId(TENANT_ID)
				.email(request.email())
				.passwordHash(passwordEncoder.encode(request.password()))
				.name(request.name())
				.phone(request.phone())
				.gender(request.gender())
				.birthDate(request.birthDate())
				.marketingConsent(request.marketingConsentOrFalse())
				// 수신거부 링크 토큰은 가입 시 항상 발급한다(동의 여부와 무관).
				.unsubscribeToken(java.util.UUID.randomUUID().toString())
				.status(MemberStatus.ACTIVE)
				.build();
		Member saved = memberRepository.save(member);
		log.info("회원가입: id={} email={} marketingConsent={}",
				saved.getId(), saved.getEmail(), saved.isMarketingConsent());
		return saved;
	}

	/**
	 * 수신거부(옵트아웃). 토큰으로 회원을 찾아 마케팅 수신 동의를 내린다.
	 *
	 * <p>멱등하다 — 없는/이미 해지된 토큰이어도 조용히 성공 취급한다(이메일 링크 클릭이므로
	 * 존재 여부를 응답으로 흘리지 않는다).
	 */
	@Transactional
	public void unsubscribe(String token) {
		if (token == null || token.isBlank()) {
			return;
		}
		memberRepository.findByUnsubscribeToken(token).ifPresent(member -> {
			member.optOutMarketing();
			log.info("마케팅 수신거부: id={}", member.getId());
		});
	}

	/**
	 * 로그인 검증. 성공하면 회원을 돌려준다(토큰 발급은 상위 계층이 한다).
	 *
	 * <p>이메일 없음·비밀번호 틀림·비활성 계정을 모두 같은 {@link UnauthorizedException} 으로
	 * 던진다 — 어느 쪽인지 흘리면 가입 여부·계정 상태를 확인해 주는 오라클이 된다.
	 */
	@Transactional(readOnly = true)
	public Member authenticate(LoginRequest request) {
		Member member = memberRepository.findByTenantIdAndEmail(TENANT_ID, request.email())
				.orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));
		if (!member.canLogin()
				|| !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
			throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
		}
		return member;
	}

	/** 인증 필터가 토큰의 회원 id 로 회원을 로드할 때 쓴다. */
	@Transactional(readOnly = true)
	public Member getActiveMember(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.filter(m -> TENANT_ID.equals(m.getTenantId()))
				.orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다. id=" + memberId));
		return member;
	}
}
