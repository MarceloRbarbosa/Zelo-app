package br.com.zelo.platform.assistance;

import static br.com.zelo.platform.SecurityTestSupport.coordinator;
import static br.com.zelo.platform.SecurityTestSupport.volunteer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import br.com.zelo.platform.ActiveAccountsTestConfiguration;
import br.com.zelo.platform.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ActiveAccountsTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class AssistanceApiIT {

	private static final String URL = "/api/v1/assistance";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE assistance_items, beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role)
				VALUES ('Clara', 'clara@zelo.local', 'x', 'COORDINATOR', 'ROLE_COORDINATOR')
				""");
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, document_number, birth_date, phone_number, status)
				VALUES ('Ana', '22900000001', '2012-01-01', '22900000001', 'ACTIVE')
				""");
	}

	@Test
	void receiptsAndDeliveriesMoveTheItemBalance() throws Exception {
		post("/items", "{\"name\": \"Cesta básica\"}").andExpect(status().isCreated());
		post("/receipts", """
				{"itemId": 1, "quantity": 20, "receivedAt": "2026-11-10", "donor": "Mercado Tamoios"}
				""").andExpect(status().isCreated()).andExpect(jsonPath("$.itemName").value("Cesta básica"));
		post("/deliveries", """
				{"itemId": 1, "beneficiaryId": 1, "quantity": 2, "deliveredAt": "2026-11-12"}
				""").andExpect(status().isCreated()).andExpect(jsonPath("$.beneficiaryName").value("Ana"));

		mockMvc.perform(get(URL + "/items").with(coordinator(1)))
			.andExpect(jsonPath("$[0].received").value(20))
			.andExpect(jsonPath("$[0].delivered").value(2))
			.andExpect(jsonPath("$[0].balance").value(18));
		mockMvc.perform(get(URL + "/deliveries").with(coordinator(1)).param("beneficiaryId", "1"))
			.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get(URL + "/receipts").with(coordinator(1)).param("from", "2026-11-01").param("to",
				"2026-11-30"))
			.andExpect(jsonPath("$[0].donor").value("Mercado Tamoios"));
		assertThat(jdbcTemplate.queryForObject("SELECT recorded_by FROM assistance_deliveries", Long.class))
			.isEqualTo(1L);
		mockMvc.perform(get(URL + "/receipts").with(coordinator(1)).param("from", "2026-11-01").param("to",
				"2026-11-30"))
			.andExpect(jsonPath("$[0].recordedBy").value("Clara"));
	}

	@Test
	void deliveriesCanBeSearchedByBeneficiaryNameAndItem() throws Exception {
		post("/items", "{\"name\": \"Cesta básica\"}");
		post("/items", "{\"name\": \"Kit de higiene\"}");
		post("/deliveries", """
				{"itemId": 1, "beneficiaryId": 1, "quantity": 1, "deliveredAt": "2026-11-12"}
				""");
		post("/deliveries", """
				{"itemId": 2, "beneficiaryId": 1, "quantity": 1, "deliveredAt": "2026-11-13"}
				""");

		mockMvc.perform(get(URL + "/deliveries").with(coordinator(1))
			.param("from", "2026-11-01")
			.param("to", "2026-11-30")
			.param("beneficiaryName", "ANA")
			.param("itemId", "2"))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].itemName").value("Kit de higiene"))
			.andExpect(jsonPath("$[0].recordedBy").value("Clara"));
		mockMvc.perform(get(URL + "/deliveries").with(coordinator(1))
			.param("from", "2026-11-01")
			.param("to", "2026-11-30")
			.param("beneficiaryName", "bruno"))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void volunteersHaveNoAccess() throws Exception {
		mockMvc.perform(get(URL + "/items").with(volunteer(1))).andExpect(status().isForbidden());
	}

	@Test
	void inactiveItemRejectsNewMovements() throws Exception {
		post("/items", "{\"name\": \"Kit de higiene\", \"active\": false}").andExpect(status().isCreated());

		post("/receipts", """
				{"itemId": 1, "quantity": 5, "receivedAt": "2026-11-10"}
				""").andExpect(status().isConflict());
	}

	private ResultActions post(String path, String body) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.post(URL + path)
			.with(coordinator(1))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

}
