package br.com.zelo.platform.program;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static br.com.zelo.platform.SecurityTestSupport.volunteer;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.zelo.platform.ActiveAccountsTestConfiguration;
import br.com.zelo.platform.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ActiveAccountsTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentApiIT {

	private static final String URL = "/api/v1/activities/1/enrollments";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role)
				VALUES ('Vera', 'vera@zelo.local', 'x', 'VOLUNTEER', 'ROLE_VOLUNTEER')
				""");
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, birth_date, phone_number, status) VALUES
				('Bruno', '2012-01-01', '22900000002', 'ACTIVE'),
				('Ana', '2012-01-01', '22900000001', 'ACTIVE'),
				('Eva', '2012-01-01', '22900000005', 'INACTIVE')
				""");
		// Monday class starting on Monday 2026-11-02, with Vera responsible
		jdbcTemplate.update("INSERT INTO activities (title, start_date) VALUES ('Capoeira', '2026-11-02')");
		jdbcTemplate.update("""
				INSERT INTO activity_schedules (activity_id, day_of_week, start_time, end_time)
				VALUES (1, 'MONDAY', '14:00', '15:00')
				""");
		jdbcTemplate.update("INSERT INTO activity_staff (activity_id, staff_id) VALUES (1, 1)");
	}

	@Test
	void enrollsBeneficiariesAndListsThemByName() throws Exception {
		enroll(1).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"));
		enroll(2).andExpect(status().isCreated());

		mockMvc.perform(get(URL).with(volunteer(1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].fullName").value("Ana"))
			.andExpect(jsonPath("$[1].fullName").value("Bruno"));
	}

	@Test
	void rejectsDuplicateAndInactiveBeneficiaries() throws Exception {
		enroll(1).andExpect(status().isCreated());

		enroll(1).andExpect(status().isConflict());
		enroll(3).andExpect(status().isNotFound());
	}

	@Test
	void leavingKeepsHistoryAndBlocksAttendanceUntilReenrolled() throws Exception {
		enroll(1);
		jdbcTemplate.update("""
				INSERT INTO attendances (activity_id, beneficiary_id, attendance_date, is_present)
				VALUES (1, 1, '2026-11-02', true)
				""");

		mockMvc
			.perform(put(URL + "/1").with(admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"LEFT\", \"leftAt\": \"2026-11-05\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("LEFT"))
			.andExpect(jsonPath("$.leftAt").value("2026-11-05"));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM attendances", Integer.class)).isEqualTo(1);
		attendance("2026-11-09").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Beneficiaries not enrolled in the activity: [1]"));

		enroll(1).andExpect(status().isCreated()).andExpect(jsonPath("$.leftAt").isEmpty());
		attendance("2026-11-09").andExpect(status().isCreated());
	}

	@Test
	void diaryShowsClassDatesMarksAndRate() throws Exception {
		enroll(1);
		enroll(2);
		attendance("2026-11-02").andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/activities/1/diary").with(volunteer(1))
			.param("from", "2026-11-01")
			.param("to", "2026-11-15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.dates.length()").value(2))
			.andExpect(jsonPath("$.dates[0]").value("2026-11-02"))
			.andExpect(jsonPath("$.dates[1]").value("2026-11-09"))
			.andExpect(jsonPath("$.students[0].fullName").value("Ana"))
			.andExpect(jsonPath("$.students[0].attendanceRate").isEmpty())
			.andExpect(jsonPath("$.students[1].fullName").value("Bruno"))
			.andExpect(jsonPath("$.students[1].attendanceRate").value(100))
			.andExpect(jsonPath("$.students[1].marks[0].present").value(true));
	}

	@Test
	void volunteerWhoIsNotResponsibleCannotOpenDiaryOrTakeAttendance() throws Exception {
		enroll(1);

		mockMvc.perform(get("/api/v1/activities/1/diary").with(volunteer(99))
			.param("from", "2026-11-01")
			.param("to", "2026-11-15")).andExpect(status().isForbidden());
		mockMvc
			.perform(post("/api/v1/attendances/batch").with(volunteer(99))
				.contentType(MediaType.APPLICATION_JSON)
				.content(attendanceBody("2026-11-02")))
			.andExpect(status().isForbidden());
	}

	private ResultActions enroll(long beneficiaryId) throws Exception {
		return mockMvc.perform(post(URL).with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"beneficiaryId\": " + beneficiaryId + "}"));
	}

	// Beneficiary 1 (Bruno) present on the given date.
	private ResultActions attendance(String date) throws Exception {
		return mockMvc.perform(post("/api/v1/attendances/batch").with(volunteer(1))
			.contentType(MediaType.APPLICATION_JSON)
			.content(attendanceBody(date)));
	}

	private static String attendanceBody(String date) {
		return """
				{"activityId": 1, "attendanceDate": "%s", "entries": [{"beneficiaryId": 1, "present": true}]}
				""".formatted(date);
	}

}
