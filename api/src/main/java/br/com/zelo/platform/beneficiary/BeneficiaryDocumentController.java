package br.com.zelo.platform.beneficiary;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// Documents of children and families: only admins and coordinators see or change them.
@RestController
@RequestMapping("/api/v1/beneficiaries/{beneficiaryId}/documents")
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
public class BeneficiaryDocumentController {

	private final BeneficiaryDocumentService service;

	public BeneficiaryDocumentController(BeneficiaryDocumentService service) {
		this.service = service;
	}

	@GetMapping
	public List<DocumentResponse> list(@PathVariable Long beneficiaryId) {
		return service.list(beneficiaryId);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public DocumentResponse upload(@PathVariable Long beneficiaryId, @RequestParam String title,
			@RequestParam MultipartFile file, Authentication authentication) {
		return service.upload(beneficiaryId, title, file, Long.valueOf(authentication.getName()));
	}

	@GetMapping("/{documentId}/content")
	public ResponseEntity<byte[]> content(@PathVariable Long beneficiaryId, @PathVariable Long documentId) {
		DocumentContent content = service.content(beneficiaryId, documentId);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(content.contentType()))
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(content.fileName(), StandardCharsets.UTF_8).build().toString())
			.header("X-Content-Type-Options", "nosniff")
			.body(content.bytes());
	}

	@DeleteMapping("/{documentId}")
	public ResponseEntity<Void> remove(@PathVariable Long beneficiaryId, @PathVariable Long documentId) {
		service.remove(beneficiaryId, documentId);
		return ResponseEntity.noContent().build();
	}

}
