package io.github.jeongkyuchoi.hotel.erp.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.jeongkyuchoi.hotel.erp.TestcontainersConfiguration;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 회원 인증 REST 통합테스트 (D-008, D-009, D-010).
 *
 * <p>회원가입 → 로그인 → 토큰으로 보호 경로 접근의 전 흐름과, 무토큰·오토큰·중복가입·오자격
 * 증명의 오류 매핑을 실제 MySQL(Testcontainers) 위에서 검증한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

	private static final String EMAIL = "gil@example.com";
	private static final String PASSWORD = "password123!";

	@Autowired private MockMvc mockMvc;
	@Autowired private MemberRepository memberRepository;

	@AfterEach
	void cleanup() {
		memberRepository.deleteAll();
	}

	private String signupJson(String email, String password) {
		return """
				{"email":"%s","password":"%s","name":"홍길동","phone":"010-1234-5678","gender":"MALE","birthDate":"1990-01-01"}
				""".formatted(email, password);
	}

	private String loginJson(String email, String password) {
		return """
				{"email":"%s","password":"%s"}
				""".formatted(email, password);
	}

	private void signup(String email, String password) throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson(email, password)))
				.andExpect(status().isCreated());
	}

	private String loginAndGetToken(String email, String password) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(email, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	@Test
	@DisplayName("회원가입 → 201, 프로필 반환(비밀번호 해시 미노출)")
	void signup_created() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson(EMAIL, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.name").value("홍길동"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	@DisplayName("회원가입 잘못된 휴대전화 → 400 VALIDATION")
	void signup_invalidPhone_validationError() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson(EMAIL, PASSWORD).replace("010-1234-5678", "02-1234-5678")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.phone").isNotEmpty());
	}

	@Test
	@DisplayName("중복 이메일 회원가입 → 409 CONFLICT")
	void signup_duplicate_conflict() throws Exception {
		signup(EMAIL, PASSWORD);
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson(EMAIL, "another123!")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONFLICT"));
	}

	@Test
	@DisplayName("로그인 → 200, Bearer 토큰 발급")
	void login_issuesToken() throws Exception {
		signup(EMAIL, PASSWORD);
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(EMAIL, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").value(3600));
	}

	@Test
	@DisplayName("잘못된 비밀번호 로그인 → 401 (이메일/비번 구분 안 함)")
	void login_wrongPassword_unauthorized() throws Exception {
		signup(EMAIL, PASSWORD);
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(EMAIL, "wrongpassword")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("토큰으로 /api/me → 200, 내 프로필")
	void me_withToken_returnsProfile() throws Exception {
		signup(EMAIL, PASSWORD);
		String token = loginAndGetToken(EMAIL, PASSWORD);

		mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.name").value("홍길동"));
	}

	@Test
	@DisplayName("무토큰 /api/me → 401")
	void me_noToken_unauthorized() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("무효 토큰 /api/me → 401")
	void me_invalidToken_unauthorized() throws Exception {
		mockMvc.perform(get("/api/me").header("Authorization", "Bearer not.a.valid.token"))
				.andExpect(status().isUnauthorized());
	}
}
