package br.com.zelo.platform.organization;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organization")
@PreAuthorize("hasRole('ADMIN')")
public class OrganizationController {

	private final DataRemovalService dataRemovalService;

	public OrganizationController(DataRemovalService dataRemovalService) {
		this.dataRemovalService = dataRemovalService;
	}

	@GetMapping("/data-removals")
	public List<DataRemovalResponse> listRemovals() {
		return dataRemovalService.list();
	}

	@PostMapping("/beneficiaries/{id}/data-removal")
	@ResponseStatus(HttpStatus.CREATED)
	public DataRemovalResponse removeData(@PathVariable Long id, @Valid @RequestBody DataRemovalRequest request,
			Authentication authentication) {
		return dataRemovalService.remove(id, request, Long.valueOf(authentication.getName()));
	}

}
