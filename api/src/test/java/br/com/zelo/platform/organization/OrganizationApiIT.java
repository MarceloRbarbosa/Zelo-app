package br.com.zelo.platform.organization;

import static br.com.zelo.platform.SecurityTestSupport.coordinator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.zelo.platform.ActiveAccountsTestConfiguration;
import br.com.zelo.platform.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ActiveAccountsTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class OrganizationApiIT {

	private static final String REMOVE = "/api/v1/organization/beneficiaries/1/data-removal";

	private static final String NOTE = "{\"note\": \"Pedido da mãe, por escrito, em 16/11/2026\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role)
				VALUES ('Admin', 'admin@zelo.local', 'x', 'PROFESSIONAL', 'ROLE_ADMIN')
				""");
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, document_number, birth_date, phone_number, address, status)
				VALUES ('Ana Souza', '52998224725', '2012-01-01', '22900000001', 'Rua A, 10', 'INACTIVE')
				""");
		jdbcTemplate.update("INSERT INTO activities (title, start_date) VALUES ('Capoeira', '2026-11-02')");
		jdbcTemplate.update("""
				INSERT INTO attendances (activity_id, beneficiary_id, attendance_date, is_present)
				VALUES (1, 1, '2026-11-02', true)
				""");
	}

	@Test
	void adminAnonymizesOnRequestKeepingHistoryAndAProofOfTheRemoval() throws Exception {
		mockMvc.perform(post(REMOVE).with(admin(1)).contentType(MediaType.APPLICATION_JSON).content(NOTE))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.removedBy").value("Admin"));

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT full_name, document_number, birth_date, phone_number, address, status FROM beneficiaries");
		assertThat(row).containsEntry("full_name", "Dados removidos (LGPD)")
			.containsEntry("document_number", null)
			.containsEntry("birth_date", null)
			.containsEntry("phone_number", null)
			.containsEntry("address", null)
			.containsEntry("status", "ANONYMIZED");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM attendances", Integer.class)).isEqualTo(1);

		mockMvc.perform(get("/api/v1/organization/data-removals").with(admin(1)))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].note").value("Pedido da mãe, por escrito, em 16/11/2026"));
		mockMvc.perform(get("/api/v1/beneficiaries").with(admin(1)).param("status", "ALL"))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void onlyAdminsReachTheOrganizationArea() throws Exception {
		mockMvc.perform(post(REMOVE).with(coordinator(1)).contentType(MediaType.APPLICATION_JSON).content(NOTE))
			.andExpect(status().isForbidden());
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM beneficiaries", String.class)).isEqualTo("INACTIVE");
	}

	@Test
	void theRequestNoteIsRequired() throws Exception {
		mockMvc.perform(post(REMOVE).with(admin(1)).contentType(MediaType.APPLICATION_JSON).content("{\"note\": \"\"}"))
			.andExpect(status().isBadRequest());
	}

	private static RequestPostProcessor admin(long staffId) {
		return jwt().jwt(token -> token.subject(String.valueOf(staffId)))
			.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

}
