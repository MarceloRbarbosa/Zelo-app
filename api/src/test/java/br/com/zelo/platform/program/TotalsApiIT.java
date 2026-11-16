package br.com.zelo.platform.program;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static br.com.zelo.platform.SecurityTestSupport.volunteer;
import static org.hamcrest.Matchers.nullValue;
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
class TotalsApiIT {

	private static final String URL = "/api/v1/totals";

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
				('Music', '2026-10-01', 'ACTIVE'),
				('Chess', '2026-10-01', 'FINISHED')
				""");
		jdbcTemplate.update("""
				INSERT INTO enrollments (activity_id, beneficiary_id)
				SELECT a.id, b.id FROM activities a CROSS JOIN beneficiaries b
				""");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role, status) VALUES
				('Vera', 'vera@zelo.local', 'x', 'VOLUNTEER', 'ROLE_VOLUNTEER', 'ACTIVE'),
				('Caio', 'caio@zelo.local', 'x', 'VOLUNTEER', 'ROLE_VOLUNTEER', 'BLOCKED')
				""");
		jdbcTemplate.update("INSERT INTO activity_staff (activity_id, staff_id) VALUES (2, 1)");
	}

	@Test
	void countsActiveRecordsOnly() throws Exception {
		// Ana and Bruno in Reading and Music; Eva is inactive and Chess is finished.
		mockMvc.perform(get(URL).with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activeBeneficiaries").value(2))
			.andExpect(jsonPath("$.activeActivities").value(2))
			.andExpect(jsonPath("$.activeEnrollments").value(4))
			.andExpect(jsonPath("$.activeStaff").value(1));
	}

	@Test
	void volunteerCountsOnlyTheirActivitiesWithoutTheTeam() throws Exception {
		mockMvc.perform(get(URL).with(volunteer(1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activeActivities").value(1))
			.andExpect(jsonPath("$.activeEnrollments").value(2))
			.andExpect(jsonPath("$.activeStaff").value(nullValue()));
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
	}

}
