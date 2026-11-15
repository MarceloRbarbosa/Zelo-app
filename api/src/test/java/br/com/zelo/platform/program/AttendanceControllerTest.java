package br.com.zelo.platform.program;

import static br.com.zelo.platform.SecurityTestSupport.admin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttendanceController.class)
class AttendanceControllerTest {

	private static final String URL = "/api/v1/attendances/batch";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AttendanceService service;

	@Test
	void invalidBodyReturnsFieldErrors() throws Exception {
		String body = """
				{"activityId": 1, "attendanceDate": "2999-01-01", "entries": [{"present": true}]}
				""";

		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.attendanceDate").exists())
			.andExpect(jsonPath("$.errors['entries[0].beneficiaryId']").exists());
	}

	@Test
	void databaseConstraintViolationReturnsConflictWithoutInternalDetails() throws Exception {
		when(service.registerBatch(any()))
			.thenThrow(new DataIntegrityViolationException("duplicate key uk_attendances_activity_beneficiary_date"));
		String body = """
				{"activityId": 1, "attendanceDate": "2026-11-10", "entries": [{"beneficiaryId": 10, "present": true}]}
				""";

		mockMvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Request conflicts with existing data"));
	}

}
