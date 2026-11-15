package br.com.zelo.platform.program;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static org.assertj.core.api.Assertions.assertThat;
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
class ActivityApiIT {

	private static final String URL = "/api/v1/activities";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries RESTART IDENTITY");
	}

	@Test
	void createPersistsActivityAsActive() throws Exception {
		String body = """
				{"title": "Reading workshop", "recurrencePattern": "WEEKLY_MON_WED", "startDate": "2026-11-02"}
				""";

		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", URL + "/1"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.leadStaffId").isEmpty());

		assertThat(jdbcTemplate.queryForObject("SELECT title FROM activities WHERE id = 1", String.class))
			.isEqualTo("Reading workshop");
	}

	@Test
	void listReturnsOnlyActiveActivitiesOrderedByTitle() throws Exception {
		jdbcTemplate.update("""
				INSERT INTO activities (title, start_date, status) VALUES
				('Music', '2026-11-02', 'ACTIVE'),
				('Chess', '2026-03-02', 'FINISHED'),
				('Art', '2026-11-02', 'ACTIVE')
				""");

		mockMvc.perform(get(URL).with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].title").value("Art"))
			.andExpect(jsonPath("$[1].title").value("Music"));
	}

}
