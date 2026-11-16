package br.com.zelo.platform.program;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/activities/{activityId}/enrollments")
public class EnrollmentController {

	private final EnrollmentService service;

	public EnrollmentController(EnrollmentService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping
	public List<EnrollmentResponse> list(@PathVariable Long activityId, Authentication authentication) {
		return service.list(activityId, CurrentStaff.from(authentication));
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EnrollmentResponse enroll(@PathVariable Long activityId, @Valid @RequestBody EnrollmentRequest request) {
		return service.enroll(activityId, request);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PutMapping("/{beneficiaryId}")
	public EnrollmentResponse changeStatus(@PathVariable Long activityId, @PathVariable Long beneficiaryId,
			@Valid @RequestBody EnrollmentStatusRequest request) {
		return service.changeStatus(activityId, beneficiaryId, request);
	}

}
