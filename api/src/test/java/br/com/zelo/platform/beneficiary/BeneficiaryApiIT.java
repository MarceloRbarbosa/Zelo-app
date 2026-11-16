package br.com.zelo.platform.beneficiary;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static br.com.zelo.platform.SecurityTestSupport.volunteer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import br.com.zelo.platform.ActiveAccountsTestConfiguration;
import br.com.zelo.platform.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ActiveAccountsTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class BeneficiaryApiIT {

	private static final String URL = "/api/v1/beneficiaries";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries RESTART IDENTITY CASCADE");
	}

	@Test
	void createPersistsBeneficiary() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body("12345678901")))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", URL + "/1"))
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		assertThat(jdbcTemplate.queryForObject("SELECT full_name FROM beneficiaries WHERE id = 1", String.class))
			.isEqualTo("Ana Souza");
	}

	@Test
	void createWithDuplicateDocumentReturnsConflict() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body("12345678901")))
			.andExpect(status().isCreated());

		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body("12345678901")))
			.andExpect(status().isConflict());
	}

	@Test
	void deleteKeepsRowAsInactiveAndHidesIt() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body(null)))
			.andExpect(status().isCreated());

		mockMvc.perform(delete(URL + "/1").with(admin())).andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject("SELECT status FROM beneficiaries WHERE id = 1", String.class))
			.isEqualTo("INACTIVE");
		mockMvc.perform(get(URL + "/1").with(admin())).andExpect(status().isNotFound());
		mockMvc.perform(get(URL).with(admin())).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void searchFiltersByNameDocumentAndActivityAndSortsByName() throws Exception {
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, document_number, birth_date, phone_number, status) VALUES
				('Ana Souza', '11111111111', '2012-01-01', '22900000001', 'ACTIVE'),
				('Bruno Souza', NULL, '2012-01-01', '22900000002', 'ACTIVE'),
				('Carla Dias', NULL, '2012-01-01', '22900000003', 'ACTIVE'),
				('Davi Souza', NULL, '2012-01-01', '22900000004', 'INACTIVE')
				""");
		jdbcTemplate.update("INSERT INTO activities (title, start_date) VALUES ('Capoeira', '2026-11-02')");
		jdbcTemplate.update("""
				INSERT INTO enrollments (activity_id, beneficiary_id, status, left_at) VALUES
				(1, 1, 'ACTIVE', NULL), (1, 3, 'ACTIVE', NULL), (1, 2, 'LEFT', '2026-11-05')
				""");

		mockMvc.perform(get(URL).with(volunteer()).param("name", "souza").param("order", "DESC"))
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].fullName").value("Bruno Souza"))
			.andExpect(jsonPath("$[1].fullName").value("Ana Souza"));
		mockMvc.perform(get(URL).with(admin()).param("documentNumber", "11111111111"))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].fullName").value("Ana Souza"));
		mockMvc.perform(get(URL).with(admin()).param("activityId", "1"))
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].fullName").value("Ana Souza"))
			.andExpect(jsonPath("$[1].fullName").value("Carla Dias"));
	}

	@Test
	void searchTreatsPercentAndUnderscoreLiterally() throws Exception {
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, birth_date, phone_number, status)
				VALUES ('Ana Souza', '2012-01-01', '22900000001', 'ACTIVE')
				""");

		mockMvc.perform(get(URL).with(admin()).param("name", "%")).andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get(URL).with(admin()).param("name", "_")).andExpect(jsonPath("$.length()").value(0));
	}

	private static String body(String documentNumber) {
		String document = documentNumber == null ? "null" : "\"" + documentNumber + "\"";
		return """
				{"fullName": "Ana Souza", "documentNumber": %s, "birthDate": "2012-03-10", "phoneNumber": "22999990000"}
				""".formatted(document);
	}

}
