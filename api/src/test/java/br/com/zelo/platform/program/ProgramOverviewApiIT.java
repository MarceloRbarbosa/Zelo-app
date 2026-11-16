package br.com.zelo.platform.program;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static br.com.zelo.platform.SecurityTestSupport.volunteer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.zelo.platform.ActiveAccountsTestConfiguration;
import br.com.zelo.platform.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ActiveAccountsTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class ProgramOverviewApiIT {

	private static final String URL = "/api/v1/program-overview";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, document_number, birth_date, phone_number, status) VALUES
				('Ana', '22900000001', '2012-01-01', '22900000001', 'ACTIVE'),
				('Bruno', '22900000002', '2012-01-01', '22900000002', 'ACTIVE'),
				('Eva', '22900000005', '2012-01-01', '22900000005', 'INACTIVE')
				""");
		jdbcTemplate.update("""
				INSERT INTO activities (title, start_date, status) VALUES
				('Reading', '2026-10-01', 'ACTIVE'),
				('Music', '2026-10-01', 'ACTIVE')
				""");
		// Both meet on Mondays; Reading also on Wednesdays.
		jdbcTemplate.update("""
				INSERT INTO activity_schedules (activity_id, day_of_week, start_time, end_time) VALUES
				(1, 'MONDAY', '14:00', '15:00'), (1, 'WEDNESDAY', '14:00', '15:00'), (2, 'MONDAY', '16:00', '17:00')
				""");
		jdbcTemplate.update("""
				INSERT INTO enrollments (activity_id, beneficiary_id)
				SELECT a.id, b.id FROM activities a CROSS JOIN beneficiaries b
				""");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role)
				VALUES ('Vera', 'vera@zelo.local', 'x', 'VOLUNTEER', 'ROLE_VOLUNTEER')
				""");
		jdbcTemplate.update("INSERT INTO activity_staff (activity_id, staff_id) VALUES (2, 1)");
	}

	@Test
	void summarizesAttendanceStudentsAndDepartures() throws Exception {
		// Monday 2026-11-02: Reading 1 of 2 present, Music 2 of 2 present.
		attendance(1, 1, "2026-11-02", true);
		attendance(1, 2, "2026-11-02", false);
		attendance(2, 1, "2026-11-02", true);
		attendance(2, 2, "2026-11-02", true);
		jdbcTemplate.update("UPDATE enrollments SET status = 'LEFT', left_at = '2026-11-03' "
				+ "WHERE activity_id = 2 AND beneficiary_id = 2");

		mockMvc.perform(get(URL).with(admin()).param("from", "2026-11-02").param("to", "2026-11-04"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.attendanceRate").value(75))
			// Ana and Bruno in Reading, Ana in Music; Eva is inactive.
			.andExpect(jsonPath("$.activeEnrollments").value(3))
			.andExpect(jsonPath("$.leftEnrollments").value(1))
			.andExpect(jsonPath("$.activities.length()").value(2))
			.andExpect(jsonPath("$.activities[0].title").value("Music"))
			.andExpect(jsonPath("$.activities[0].attendanceRate").value(100))
			.andExpect(jsonPath("$.activities[1].title").value("Reading"))
			.andExpect(jsonPath("$.activities[1].attendanceRate").value(50));
	}

	@Test
	void volunteerSeesOnlyTheirActivities() throws Exception {
		mockMvc.perform(get(URL).with(volunteer(1)).param("from", "2026-11-02").param("to", "2026-11-04"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activities.length()").value(1))
			.andExpect(jsonPath("$.activities[0].title").value("Music"));
	}

	@Test
	void rejectsInvertedPeriod() throws Exception {
		mockMvc.perform(get(URL).with(admin()).param("from", "2026-11-04").param("to", "2026-11-02"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get(URL).param("from", "2026-11-02").param("to", "2026-11-04"))
			.andExpect(status().isUnauthorized());
	}

	private void attendance(long activityId, long beneficiaryId, String date, boolean present) {
		jdbcTemplate.update("""
				INSERT INTO attendances (activity_id, beneficiary_id, attendance_date, is_present)
				VALUES (?, ?, CAST(? AS DATE), ?)
				""", activityId, beneficiaryId, date, present);
	}

}
