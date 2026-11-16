package br.com.zelo.platform.beneficiary;

import static br.com.zelo.platform.SecurityTestSupport.coordinator;
import static br.com.zelo.platform.SecurityTestSupport.volunteer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import br.com.zelo.platform.ActiveAccountsTestConfiguration;
import br.com.zelo.platform.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ActiveAccountsTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class BeneficiaryDocumentApiIT {

	private static final String URL = "/api/v1/beneficiaries/1/documents";

	private static final byte[] PDF = "%PDF-1.7\n%fake but well-formed header\n".getBytes(StandardCharsets.US_ASCII);

	@TempDir
	static Path documents;

	@DynamicPropertySource
	static void storage(DynamicPropertyRegistry registry) {
		registry.add("zelo.storage.documents-dir", documents::toString);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE beneficiaries, staff_members RESTART IDENTITY CASCADE");
		jdbcTemplate.update("""
				INSERT INTO staff_members (full_name, email, password_hash, member_type, role)
				VALUES ('Clara', 'clara@zelo.local', 'x', 'COORDINATOR', 'ROLE_COORDINATOR')
				""");
		jdbcTemplate.update("""
				INSERT INTO beneficiaries (full_name, document_number, birth_date, phone_number, status)
				VALUES ('Ana', '52998224725', '2012-01-01', '22900000001', 'ACTIVE')
				""");
	}

	@Test
	void coordinatorUploadsListsDownloadsAndRemovesAPdf() throws Exception {
		mockMvc.perform(multipart(URL).file(new MockMultipartFile("file", "comprovante.pdf", "application/pdf", PDF))
			.param("title", "Comprovante de residência")
			.with(coordinator(1)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.contentType").value("application/pdf"))
			.andExpect(jsonPath("$.fileName").value("comprovante.pdf"));

		mockMvc.perform(get(URL).with(coordinator(1)))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].title").value("Comprovante de residência"));
		mockMvc.perform(get(URL + "/1/content").with(coordinator(1)))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"))
			.andExpect(content().bytes(PDF));

		mockMvc.perform(delete(URL + "/1").with(coordinator(1))).andExpect(status().isNoContent());
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM beneficiary_documents", Integer.class)).isZero();
		try (var files = Files.list(documents)) {
			assertThat(files).isEmpty();
		}
	}

	@Test
	void rejectsFilesThatAreNotPdfOrImages() throws Exception {
		mockMvc.perform(multipart(URL)
			.file(new MockMultipartFile("file", "foto.pdf", "application/pdf", "<html>".getBytes(StandardCharsets.UTF_8)))
			.param("title", "Disfarçado")
			.with(coordinator(1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Only PDF, JPG and PNG files are accepted"));
	}

	@Test
	void volunteersHaveNoAccess() throws Exception {
		mockMvc.perform(get(URL).with(volunteer(1))).andExpect(status().isForbidden());
	}

}
