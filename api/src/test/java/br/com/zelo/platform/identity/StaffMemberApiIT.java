package br.com.zelo.platform.identity;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.zelo.platform.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class StaffMemberApiIT {

	private static final String URL = "/api/v1/staff-members";

	private static final String BODY = """
			{"fullName": "Bia Lima", "email": "Bia@Zelo.org", "password": "secret123",
			 "memberType": "VOLUNTEER", "role": "ROLE_VOLUNTEER"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries, staff_members RESTART IDENTITY CASCADE");
	}

	@Test
	void adminCreatesStaffWithHashedPasswordAndNormalizedEmail() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("bia@zelo.org"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		String hash = jdbcTemplate.queryForObject("SELECT password_hash FROM staff_members", String.class);
		assertThat(hash).startsWith("$2a$10$").doesNotContain("secret123");
	}

	@Test
	void duplicateEmailReturnsConflict() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated());

		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict());
	}

	@Test
	void beneficiaryRoleIsNotAvailableYet() throws Exception {
		mockMvc
			.perform(post(URL).with(admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(BODY.replace("ROLE_VOLUNTEER", "ROLE_BENEFICIARY")))
			.andExpect(status().isBadRequest());
	}

}
