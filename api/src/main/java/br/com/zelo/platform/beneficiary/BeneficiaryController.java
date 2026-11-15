package br.com.zelo.platform.beneficiary;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiaryController {

	private final BeneficiaryService service;

	public BeneficiaryController(BeneficiaryService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping
	public List<BeneficiaryResponse> list() {
		return service.listActive();
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping("/{id}")
	public BeneficiaryResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PostMapping
	public ResponseEntity<BeneficiaryResponse> create(@Valid @RequestBody BeneficiaryRequest request) {
		BeneficiaryResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/v1/beneficiaries/" + created.id())).body(created);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PutMapping("/{id}")
	public BeneficiaryResponse update(@PathVariable Long id, @Valid @RequestBody BeneficiaryRequest request) {
		return service.update(id, request);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.deactivate(id);
		return ResponseEntity.noContent().build();
	}

}
