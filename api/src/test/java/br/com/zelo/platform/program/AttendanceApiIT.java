package br.com.zelo.platform.program;

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

import br.com.zelo.platform.ActiveAccountsTestConfiguration;
import br.com.zelo.platform.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ActiveAccountsTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class AttendanceApiIT {

	private static final String URL = "/api/v1/attendances/batch";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries RESTART IDENTITY");
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, birth_date, phone_number, status) VALUES
				('Ana Souza', '2012-03-10', '22999990000', 'ACTIVE'),
				('Bruno Reis', '2011-07-21', '22988880000', 'ACTIVE'),
				('Carla Dias', '2013-01-15', '22977770000', 'INACTIVE')
				""");
		jdbcTemplate.update("INSERT INTO activities (title, start_date) VALUES ('Reading workshop', '2026-11-02')");
	}

	@Test
	void registerBatchPersistsAllEntries() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(batch("2026-11-10", 1, 2)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.registered").value(2))
			.andExpect(jsonPath("$.present").value(1))
			.andExpect(jsonPath("$.absent").value(1));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM attendances", Integer.class)).isEqualTo(2);
	}

	@Test
	void registerBatchTwiceOnSameDateReturnsConflict() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(batch("2026-11-10", 1, 2)))
			.andExpect(status().isCreated());

		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(batch("2026-11-10", 1, 2)))
			.andExpect(status().isConflict());
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM attendances", Integer.class)).isEqualTo(2);
	}

	@Test
	void registerBatchWithInactiveBeneficiarySavesNothing() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(batch("2026-11-10", 1, 3)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Beneficiaries not found or inactive: [3]"));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM attendances", Integer.class)).isZero();
	}

	// First beneficiary is present, second is absent.
	private static String batch(String date, long firstId, long secondId) {
		return """
				{"activityId": 1, "attendanceDate": "%s", "entries": [
				  {"beneficiaryId": %d, "present": true},
				  {"beneficiaryId": %d, "present": false}
				]}
				""".formatted(date, firstId, secondId);
	}

}
