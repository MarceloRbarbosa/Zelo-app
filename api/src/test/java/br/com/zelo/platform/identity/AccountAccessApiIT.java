package br.com.zelo.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.zelo.platform.TestcontainersConfiguration;
import tools.jackson.databind.json.JsonMapper;

// End-to-end with real tokens: provisional password, first-login password change, profile and blocking.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AccountAccessApiIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String adminToken;

	@BeforeEach
	void setUp() throws Exception {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role)
				VALUES ('Admin', 'admin@zelo.org', ?, 'PROFESSIONAL', 'ROLE_ADMIN')
				""", passwordEncoder.encode("admin-pass"));
		adminToken = login("admin@zelo.org", "admin-pass");
		mockMvc.perform(authorized(post("/api/v1/staff-members"), adminToken).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"fullName": "Bia Lima", "email": "bia@zelo.org", "password": "temporaria1",
					 "memberType": "VOLUNTEER", "role": "ROLE_VOLUNTEER"}
					""")).andExpect(status().isCreated());
	}

	@Test
	void provisionalPasswordAllowsOnlyProfileUntilItIsChanged() throws Exception {
		String body = loginResponse("bia@zelo.org", "temporaria1");
		String token = JsonMapper.shared().readTree(body).get("token").asString();
		assertThat(JsonMapper.shared().readTree(body).get("mustChangePassword").asBoolean())
			.isTrue();

		mockMvc.perform(authorized(get("/api/v1/beneficiaries"), token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("Password change required"));
		mockMvc.perform(authorized(get("/api/v1/me"), token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mustChangePassword").value(true));

		changePassword(token, "errada123", "minha-senha-nova").andExpect(status().isBadRequest());
		changePassword(token, "temporaria1", "temporaria1").andExpect(status().isBadRequest());
		changePassword(token, "temporaria1", "minha-senha-nova").andExpect(status().isNoContent());

		mockMvc.perform(authorized(get("/api/v1/beneficiaries"), token)).andExpect(status().isOk());
		login("bia@zelo.org", "minha-senha-nova");
	}

	@Test
	void blockingTakesEffectImmediatelyOnExistingToken() throws Exception {
		String token = activeVolunteerToken();
		mockMvc.perform(authorized(get("/api/v1/beneficiaries"), token)).andExpect(status().isOk());

		mockMvc.perform(authorized(post("/api/v1/staff-members/2/block"), adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("BLOCKED"));

		mockMvc.perform(authorized(get("/api/v1/beneficiaries"), token)).andExpect(status().isUnauthorized());
		mockMvc.perform(loginRequest("bia@zelo.org", "minha-senha-nova")).andExpect(status().isUnauthorized());

		mockMvc.perform(authorized(post("/api/v1/staff-members/2/unblock"), adminToken)).andExpect(status().isOk());
		login("bia@zelo.org", "minha-senha-nova");
	}

	@Test
	void adminCannotBlockOwnAccount() throws Exception {
		mockMvc.perform(authorized(post("/api/v1/staff-members/1/block"), adminToken))
			.andExpect(status().isBadRequest());
	}

	@Test
	void memberUpdatesOwnName() throws Exception {
		String token = activeVolunteerToken();

		mockMvc
			.perform(authorized(put("/api/v1/me"), token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\": \"Beatriz Lima\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Beatriz Lima"));
	}

	private String activeVolunteerToken() throws Exception {
		String provisional = login("bia@zelo.org", "temporaria1");
		changePassword(provisional, "temporaria1", "minha-senha-nova").andExpect(status().isNoContent());
		return login("bia@zelo.org", "minha-senha-nova");
	}

	private ResultActions changePassword(String token, String current,
			String next) throws Exception {
		return mockMvc.perform(authorized(put("/api/v1/me/password"), token).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"currentPassword": "%s", "newPassword": "%s"}
					""".formatted(current, next)));
	}

	private String login(String email, String password) throws Exception {
		return JsonMapper.shared().readTree(loginResponse(email, password)).get("token").asString();
	}

	private String loginResponse(String email, String password) throws Exception {
		return mockMvc.perform(loginRequest(email, password))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static MockHttpServletRequestBuilder loginRequest(String email, String password) {
		return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "password": "%s"}
				""".formatted(email, password));
	}

	private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

}
