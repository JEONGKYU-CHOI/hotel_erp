package com.hotel.erp.backoffice.security;

import com.hotel.erp.common.domain.member.StaffUser;
import com.hotel.erp.common.domain.member.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 백오피스 로그인 시 {@code staff_user} 테이블에서 계정을 찾아오는 어댑터.
 *
 * <p>스프링 시큐리티는 "아이디로 사용자를 찾아 달라"고만 요청하고, 비밀번호 비교는
 * {@code PasswordEncoder} 를 통해 <b>스프링이 직접</b> 한다. 여기서 비밀번호를
 * 비교하면 안 된다 — 직접 비교하면 타이밍 공격 방어나 해시 알고리즘 교체 지원 같은
 * 것들을 전부 잃는다.
 *
 * <p>고객 회원({@code member})은 이 경로를 타지 않는다. 부킹엔진은 JWT 로 별도
 * 인증한다(D-008). 테이블도 인증 경로도 분리돼 있다(D-014).
 */
@Service
@RequiredArgsConstructor
public class StaffUserDetailsService implements UserDetailsService {

	/** 멀티테넌시는 백로그이므로 전부 1 고정이다(D-007). */
	private static final Long TENANT_ID = 1L;

	private final StaffUserRepository staffUserRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		StaffUser staff = staffUserRepository
				.findByTenantIdAndUsernameAndActiveTrue(TENANT_ID, username)
				// 예외 메시지에 "그런 아이디 없음" 과 "비밀번호 틀림" 을 구분해 담지 않는다.
				// 구분되면 공격자가 유효한 아이디 목록을 수집할 수 있다.
				// 스프링이 이 예외를 BadCredentialsException 으로 감춰 주지만,
				// 메시지 자체도 정보를 흘리지 않게 둔다.
				.orElseThrow(() -> new UsernameNotFoundException("인증에 실패했습니다."));

		// ROLE_ 접두사는 스프링 시큐리티의 관례다. hasRole("ADMIN") 이 내부적으로
		// "ROLE_ADMIN" 을 찾기 때문에, 여기서 접두사를 빼면 권한 검사가 조용히 실패한다.
		return User.withUsername(staff.getUsername())
				.password(staff.getPasswordHash())
				.authorities(new SimpleGrantedAuthority("ROLE_" + staff.getRole().name()))
				.build();
	}
}
