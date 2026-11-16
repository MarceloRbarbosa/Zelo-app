package br.com.zelo.platform.program;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static br.com.zelo.platform.SecurityTestSupport.coordinator;
import static br.com.zelo.platform.SecurityTestSupport.volunteer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ActivityApiIT {

	private static final String URL = "/api/v1/activities";

	private static final String BODY = """
			{"title": "Capoeira", "description": "Turma infantil", "startDate": "2026-11-02",
			 "schedules": [{"dayOfWeek": "WEDNESDAY", "startTime": "14:00", "endTime": "15:00"},
			               {"dayOfWeek": "MONDAY", "startTime": "14:00", "endTime": "15:00"}],
			 "staffIds": [1, 2]}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role) VALUES
				('Vera', 'vera@zelo.local', 'x', 'VOLUNTEER', 'ROLE_VOLUNTEER'),
				('Caio', 'caio@zelo.local', 'x', 'VOLUNTEER', 'ROLE_VOLUNTEER'),
				('Lia', 'lia@zelo.local', 'x', 'VOLUNTEER', 'ROLE_VOLUNTEER')
				""");
	}

	@Test
	void coordinatorCreatesActivityWithSchedulesAndStaff() throws Exception {
		mockMvc.perform(post(URL).with(coordinator()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", URL + "/1"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.endDate").isEmpty())
			.andExpect(jsonPath("$.schedules[0].dayOfWeek").value("MONDAY"))
			.andExpect(jsonPath("$.schedules[1].dayOfWeek").value("WEDNESDAY"))
			.andExpect(jsonPath("$.staff[0].fullName").value("Caio"))
			.andExpect(jsonPath("$.staff[1].fullName").value("Vera"));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM activity_schedules", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM activity_staff", Integer.class)).isEqualTo(2);
	}

	@Test
	void createWithoutStaffOrSchedulesIsRejected() throws Exception {
		String body = """
				{"title": "Capoeira", "startDate": "2026-11-02", "schedules": [], "staffIds": []}
				""";

		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.schedules").exists())
			.andExpect(jsonPath("$.errors.staffIds").exists());
	}

	@Test
	void volunteerCannotCreateActivities() throws Exception {
		mockMvc.perform(post(URL).with(volunteer(1)).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isForbidden());
	}

	@Test
	void updateReplacesSchedulesAndStaff() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY));
		String body = """
				{"title": "Capoeira avançada", "startDate": "2026-11-02",
				 "schedules": [{"dayOfWeek": "FRIDAY", "startTime": "09:00", "endTime": "10:00"}],
				 "staffIds": [3]}
				""";

		mockMvc.perform(put(URL + "/1").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Capoeira avançada"))
			.andExpect(jsonPath("$.schedules.length()").value(1))
			.andExpect(jsonPath("$.staff[0].fullName").value("Lia"));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM activity_staff", Integer.class)).isEqualTo(1);
	}

	@Test
	void finishRecordsEndDateAndMovesActivityToFinishedList() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY));

		mockMvc
			.perform(post(URL + "/1/finish").with(admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"endDate\": \"2026-12-18\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FINISHED"))
			.andExpect(jsonPath("$.endDate").value("2026-12-18"));

		mockMvc.perform(get(URL).with(admin())).andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get(URL).with(admin()).param("status", "FINISHED")).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(put(URL + "/1").with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict());
	}

	@Test
	void classSessionsListEveryActivityClassInThePeriodForAnyMember() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY));

		// Monday and Wednesday classes from 2026-11-02 to 2026-11-08
		mockMvc.perform(get("/api/v1/class-sessions").with(volunteer(3))
			.param("from", "2026-11-01")
			.param("to", "2026-11-08"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].date").value("2026-11-02"))
			.andExpect(jsonPath("$[0].title").value("Capoeira"))
			.andExpect(jsonPath("$[1].date").value("2026-11-04"));
	}

	@Test
	void volunteerSeesOnlyActivitiesTheyAreResponsibleFor() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY));

		mockMvc.perform(get(URL).with(volunteer(1))).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get(URL + "/1").with(volunteer(1))).andExpect(status().isOk());
		mockMvc.perform(get(URL).with(volunteer(3))).andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get(URL + "/1").with(volunteer(3))).andExpect(status().isForbidden());
	}

}
