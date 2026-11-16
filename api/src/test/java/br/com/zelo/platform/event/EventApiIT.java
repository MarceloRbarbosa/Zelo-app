package br.com.zelo.platform.event;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static br.com.zelo.platform.SecurityTestSupport.coordinator;
import static br.com.zelo.platform.SecurityTestSupport.volunteer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.zelo.platform.ActiveAccountsTestConfiguration;
import br.com.zelo.platform.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ActiveAccountsTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class EventApiIT {

	private static final String URL = "/api/v1/events";

	private static final String BODY = """
			{"title": "Feira de saúde", "description": "Vacinação e orientação", "eventDate": "2026-12-05T13:00:00Z",
			 "location": "Quadra", "staffIds": [1]}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE events, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role)
				VALUES ('Vera', 'vera@zelo.local', 'x', 'VOLUNTEER', 'ROLE_VOLUNTEER')
				""");
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, document_number, birth_date, phone_number, status) VALUES
				('Bruno', '22900000002', '2012-01-01', '22900000002', 'ACTIVE'),
				('Ana', '22900000001', '2012-01-01', '22900000001', 'ACTIVE')
				""");
	}

	@Test
	void coordinatorCreatesEventAndEveryoneSeesItInThePeriod() throws Exception {
		mockMvc.perform(post(URL).with(coordinator()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", URL + "/1"))
			.andExpect(jsonPath("$.status").value("SCHEDULED"))
			.andExpect(jsonPath("$.staff[0].fullName").value("Vera"));

		mockMvc.perform(get(URL).with(volunteer(99))
			.param("from", "2026-12-01T00:00:00Z")
			.param("to", "2026-12-31T23:59:59Z")).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get(URL).with(admin())
			.param("from", "2027-01-01T00:00:00Z")
			.param("to", "2027-01-31T23:59:59Z")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void volunteerCannotCreateEvents() throws Exception {
		mockMvc.perform(post(URL).with(volunteer(1)).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isForbidden());
	}

	@Test
	void responsibleVolunteerRegistersAndMarksWhoAttended() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY));

		register(1, volunteer(1)).andExpect(status().isCreated()).andExpect(jsonPath("$.attended").value(false));
		register(2, volunteer(1)).andExpect(status().isCreated());
		register(1, volunteer(1)).andExpect(status().isConflict());

		mockMvc.perform(put(URL + "/1/participants/1").with(volunteer(1))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"attended\": true}")).andExpect(status().isOk()).andExpect(jsonPath("$.attended").value(true));

		mockMvc.perform(get(URL + "/1/participants").with(volunteer(1)))
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].fullName").value("Ana"))
			.andExpect(jsonPath("$[0].attended").value(false))
			.andExpect(jsonPath("$[1].fullName").value("Bruno"))
			.andExpect(jsonPath("$[1].attended").value(true));

		mockMvc.perform(delete(URL + "/1/participants/2").with(volunteer(1))).andExpect(status().isNoContent());
	}

	@Test
	void otherVolunteerCannotTouchParticipants() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY));

		register(1, volunteer(99)).andExpect(status().isForbidden());
		mockMvc.perform(get(URL + "/1/participants").with(volunteer(99))).andExpect(status().isForbidden());
	}

	@Test
	void canceledEventStopsRegistrationAndEditing() throws Exception {
		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY));

		mockMvc.perform(post(URL + "/1/cancel").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELED"));

		register(1, admin()).andExpect(status().isConflict());
		mockMvc.perform(put(URL + "/1").with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict());
	}

	private ResultActions register(long beneficiaryId,
			RequestPostProcessor who) throws Exception {
		return mockMvc.perform(post(URL + "/1/participants").with(who)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"beneficiaryId\": " + beneficiaryId + "}"));
	}

}
