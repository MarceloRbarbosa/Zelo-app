package br.com.zelo.platform.program;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.zelo.platform.shared.security.CurrentStaff;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

	private final ActivityService service;

	public ActivityController(ActivityService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping
	public List<ActivityResponse> list(@RequestParam(defaultValue = "ACTIVE") ActivityStatus status,
			Authentication authentication) {
		return service.list(status, CurrentStaff.from(authentication));
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping("/{id}")
	public ActivityResponse get(@PathVariable Long id, Authentication authentication) {
		return service.get(id, CurrentStaff.from(authentication));
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PostMapping
	public ResponseEntity<ActivityResponse> create(@Valid @RequestBody ActivityRequest request) {
		ActivityResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/v1/activities/" + created.id())).body(created);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PutMapping("/{id}")
	public ActivityResponse update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
		return service.update(id, request);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PostMapping("/{id}/finish")
	public ActivityResponse finish(@PathVariable Long id, @Valid @RequestBody ActivityFinishRequest request) {
		return service.finish(id, request);
	}

}
