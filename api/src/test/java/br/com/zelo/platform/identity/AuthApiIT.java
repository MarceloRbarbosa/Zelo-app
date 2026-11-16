package br.com.zelo.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import br.com.zelo.platform.TestcontainersConfiguration;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIT {

	private static final String LOGIN_URL = "/api/v1/auth/login";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		staff("coord@zelo.org", "ROLE_COORDINATOR");
		staff("vol@zelo.org", "ROLE_VOLUNTEER");
	}

	@Test
	void loginReturnsTokenAcceptedByProtectedEndpoints() throws Exception {
		String token = login("coord@zelo.org", "secret123");

		mockMvc.perform(get("/api/v1/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/beneficiaries")).andExpect(status().isUnauthorized());
	}

	@Test
	void tamperedTokenIsUnauthorized() throws Exception {
		String token = login("coord@zelo.org", "secret123");

		mockMvc.perform(get("/api/v1/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void wrongPasswordIsUnauthorizedAndUnknownEmailGetsSameAnswer() throws Exception {
		String wrongPassword = mockMvc.perform(loginRequest("coord@zelo.org", "wrong-pass"))
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String unknownEmail = mockMvc.perform(loginRequest("nobody@zelo.org", "wrong-pass"))
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(JsonMapper.shared().readTree(wrongPassword).get("detail"))
			.isEqualTo(JsonMapper.shared().readTree(unknownEmail).get("detail"));
	}

	@Test
	void fiveWrongPasswordsLockAccountEvenForCorrectPassword() throws Exception {
		for (int attempt = 1; attempt <= 5; attempt++) {
			mockMvc.perform(loginRequest("coord@zelo.org", "wrong-pass")).andExpect(status().isUnauthorized());
		}

		mockMvc.perform(loginRequest("coord@zelo.org", "secret123")).andExpect(status().isUnauthorized());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT locked_until > now() FROM staff_members WHERE email = 'coord@zelo.org'", Boolean.class))
			.isTrue();
	}

	@Test
	void volunteerCanSeeAlertsButCannotRegisterBeneficiaries() throws Exception {
		String token = login("vol@zelo.org", "secret123");

		mockMvc.perform(get("/api/v1/dropout-alerts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
		mockMvc
			.perform(post("/api/v1/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName": "Ana", "birthDate": "2012-01-01", "phoneNumber": "22999990000"}
						"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void coordinatorCannotManageStaff() throws Exception {
		String token = login("coord@zelo.org", "secret123");

		// Listing the team is allowed (to pick who is responsible for an activity); blocking is not.
		mockMvc.perform(get("/api/v1/staff-members").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/staff-members/1/block").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	private String login(String email, String password) throws Exception {
		String body = mockMvc.perform(loginRequest(email, password))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.expiresAt").exists())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonMapper.shared().readTree(body).get("token").asString();
	}

	private static org.springframework.test.web.servlet.RequestBuilder loginRequest(String email, String password) {
		return post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "password": "%s"}
				""".formatted(email, password));
	}

	private void staff(String email, String role) {
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role)
				VALUES ('Staff', ?, ?, 'VOLUNTEER', ?)
				""", email, passwordEncoder.encode("secret123"), role);
	}

}
