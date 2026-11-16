package br.com.zelo.platform.identity;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff-members")
// Admins and coordinators manage the team; StaffRole decides which members each one may create or block.
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
public class StaffMemberController {

	private final StaffMemberService service;

	public StaffMemberController(StaffMemberService service) {
		this.service = service;
	}

	@GetMapping
	public List<StaffMemberResponse> list() {
		return service.list();
	}

	@PostMapping
	public ResponseEntity<StaffMemberResponse> create(@Valid @RequestBody StaffMemberRequest request,
			Authentication authentication) {
		StaffMemberResponse created = service.create(request, StaffRole.of(authentication));
		return ResponseEntity.created(URI.create("/api/v1/staff-members/" + created.id())).body(created);
	}

	@PostMapping("/{id}/block")
	public StaffMemberResponse block(@PathVariable Long id, Authentication authentication) {
		return service.block(id, Long.valueOf(authentication.getName()), StaffRole.of(authentication));
	}

	@PostMapping("/{id}/unblock")
	public StaffMemberResponse unblock(@PathVariable Long id, Authentication authentication) {
		return service.unblock(id, StaffRole.of(authentication));
	}

}
