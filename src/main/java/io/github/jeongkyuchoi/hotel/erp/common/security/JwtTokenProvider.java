package io.github.jeongkyuchoi.hotel.erp.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 발급·검증 (D-008, D-010 — Access Token 단독).
 *
 * <p>서명은 HS256(대칭키). 토큰의 {@code subject} 에 회원 id 를 담는다. 이메일 같은 부가
 * 정보는 클레임에 넣되, <b>비밀번호 해시 등 민감정보는 절대 담지 않는다</b> — JWT 페이로드는
 * 서명될 뿐 암호화되지 않아 누구나 디코딩해 읽을 수 있다.
 */
@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final Duration validity;

	public JwtTokenProvider(JwtProperties properties) {
		// HS256 은 256bit 이상 키를 요구한다. 짧으면 여기서 예외로 기동이 실패한다.
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.validity = properties.accessTokenValidity();
	}

	/** 회원 id 를 subject 로 하는 Access Token 을 발급한다. */
	public String issue(Long memberId, String email) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(memberId))
				.claim("email", email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(validity)))
				.signWith(key)
				.compact();
	}

	/**
	 * 토큰을 검증하고 회원 id 를 꺼낸다.
	 *
	 * @throws io.jsonwebtoken.JwtException 서명 불일치·만료·형식 오류 등. 호출자(필터)가
	 *         잡아 인증하지 않고 넘긴다.
	 */
	public Long parseMemberId(String token) {
		String subject = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
		return Long.valueOf(subject);
	}

	/** 토큰 수명(초). 로그인 응답에 실어 클라이언트가 만료를 예측하게 한다. */
	public long validitySeconds() {
		return validity.toSeconds();
	}
}
