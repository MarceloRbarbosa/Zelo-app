package br.com.zelo.platform.event;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.zelo.platform.shared.security.CurrentStaff;

@RestController
@RequestMapping("/api/v1/events/{eventId}/participants")
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
public class ParticipantController {

	private final ParticipantService service;

	public ParticipantController(ParticipantService service) {
		this.service = service;
	}

	@GetMapping
	public List<ParticipantResponse> list(@PathVariable Long eventId, Authentication authentication) {
		return service.list(eventId, CurrentStaff.from(authentication));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ParticipantResponse register(@PathVariable Long eventId, @Valid @RequestBody ParticipantRequest request,
			Authentication authentication) {
		return service.register(eventId, request, CurrentStaff.from(authentication));
	}

	@PutMapping("/{beneficiaryId}")
	public ParticipantResponse markAttendance(@PathVariable Long eventId, @PathVariable Long beneficiaryId,
			@Valid @RequestBody AttendanceMarkRequest request, Authentication authentication) {
		return service.markAttendance(eventId, beneficiaryId, request, CurrentStaff.from(authentication));
	}

	@DeleteMapping("/{beneficiaryId}")
	public ResponseEntity<Void> unregister(@PathVariable Long eventId, @PathVariable Long beneficiaryId,
			Authentication authentication) {
		service.unregister(eventId, beneficiaryId, CurrentStaff.from(authentication));
		return ResponseEntity.noContent().build();
	}

}
