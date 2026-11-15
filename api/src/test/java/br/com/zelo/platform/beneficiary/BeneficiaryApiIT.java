package br.com.zelo.platform.beneficiary;

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

import br.com.zelo.platform.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
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
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries RESTART IDENTITY");
	}

	@Test
	void createPersistsBeneficiary() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("12345678901")))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", URL + "/1"))
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		assertThat(jdbcTemplate.queryForObject("SELECT full_name FROM beneficiaries WHERE id = 1", String.class))
			.isEqualTo("Ana Souza");
	}

	@Test
	void createWithDuplicateDocumentReturnsConflict() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("12345678901")))
			.andExpect(status().isCreated());

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("12345678901")))
			.andExpect(status().isConflict());
	}

	@Test
	void deleteKeepsRowAsInactiveAndHidesIt() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body(null)))
			.andExpect(status().isCreated());

		mockMvc.perform(delete(URL + "/1")).andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject("SELECT status FROM beneficiaries WHERE id = 1", String.class))
			.isEqualTo("INACTIVE");
		mockMvc.perform(get(URL + "/1")).andExpect(status().isNotFound());
		mockMvc.perform(get(URL)).andExpect(jsonPath("$.length()").value(0));
	}

	private static String body(String documentNumber) {
		String document = documentNumber == null ? "null" : "\"" + documentNumber + "\"";
		return """
				{"fullName": "Ana Souza", "documentNumber": %s, "birthDate": "2012-03-10", "phoneNumber": "22999990000"}
				""".formatted(document);
	}

}
