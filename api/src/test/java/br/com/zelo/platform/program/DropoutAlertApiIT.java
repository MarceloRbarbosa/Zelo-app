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
class DropoutAlertApiIT {

	private static final String URL = "/api/v1/dropout-alerts";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE attendances, activities, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, birth_date, phone_number, status) VALUES
				('Ana', '2012-01-01', '22900000001', 'ACTIVE'),
				('Bruno', '2012-01-01', '22900000002', 'ACTIVE'),
				('Carla', '2012-01-01', '22900000003', 'ACTIVE'),
				('Davi', '2012-01-01', '22900000004', 'ACTIVE'),
				('Eva', '2012-01-01', '22900000005', 'INACTIVE')
				""");
		jdbcTemplate.update("""
				INSERT INTO activities (title, start_date, status) VALUES
				('Reading', '2026-10-01', 'ACTIVE'),
				('Music', '2026-10-01', 'ACTIVE'),
				('Chess', '2026-10-01', 'FINISHED')
				""");
		// Everyone is enrolled in every activity; Vera (staff 1) is responsible for Music only.
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
	void alertsOnlyCurrentStreaksOfThreeOrMoreAbsences() throws Exception {
		// Ana: present, then 4 absences in a row -> alert with 4
		attendances(1, 1, "P", "F", "F", "F", "F");
		// Bruno: 3 absences, then came back -> no alert
		attendances(1, 2, "F", "F", "F", "P");
		// Carla: only the last 2 are absences -> no alert
		attendances(1, 3, "F", "P", "F", "F");
		// Davi: 3 absences in a row -> alert with 3
		attendances(1, 4, "P", "F", "F", "F");

		mockMvc.perform(get(URL).with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].beneficiaryName").value("Ana"))
			.andExpect(jsonPath("$[0].phoneNumber").value("22900000001"))
			.andExpect(jsonPath("$[0].activityTitle").value("Reading"))
			.andExpect(jsonPath("$[0].consecutiveAbsences").value(4))
			.andExpect(jsonPath("$[0].lastAttendanceDate").value("2026-11-05"))
			.andExpect(jsonPath("$[1].beneficiaryName").value("Davi"))
			.andExpect(jsonPath("$[1].consecutiveAbsences").value(3));
	}

	@Test
	void countsAbsencesPerActivity() throws Exception {
		// Ana misses 2 in Reading and 2 in Music: no single activity reaches 3
		attendances(1, 1, "F", "F");
		attendances(2, 1, "F", "F");

		mockMvc.perform(get(URL).with(admin())).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void ignoresInactiveBeneficiariesAndFinishedActivities() throws Exception {
		attendances(1, 5, "F", "F", "F");
		attendances(3, 1, "F", "F", "F");

		mockMvc.perform(get(URL).with(admin())).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void filtersByActivity() throws Exception {
		attendances(1, 1, "F", "F", "F");
		attendances(2, 2, "F", "F", "F");

		mockMvc.perform(get(URL).with(admin()).param("activityId", "2"))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].beneficiaryName").value("Bruno"));
	}

	@Test
	void ignoresBeneficiariesWhoLeftTheActivity() throws Exception {
		attendances(1, 1, "F", "F", "F");
		jdbcTemplate.update("UPDATE enrollments SET status = 'LEFT', left_at = '2026-11-04' WHERE beneficiary_id = 1");

		mockMvc.perform(get(URL).with(admin())).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void volunteerSeesOnlyAlertsOfTheirActivities() throws Exception {
		attendances(1, 1, "F", "F", "F");
		attendances(2, 2, "F", "F", "F");

		mockMvc.perform(get(URL).with(volunteer(1)))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].activityTitle").value("Music"));
	}

	// One attendance per day starting on 2026-11-01; "P" = present, "F" = absent.
	private void attendances(long activityId, long beneficiaryId, String... marks) {
		for (int day = 0; day < marks.length; day++) {
			jdbcTemplate.update("""
					INSERT INTO attendances (activity_id, beneficiary_id, attendance_date, is_present)
					VALUES (?, ?, DATE '2026-11-01' + ?, ?)
					""", activityId, beneficiaryId, day, marks[day].equals("P"));
		}
	}

}
