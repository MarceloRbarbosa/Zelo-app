package br.com.zelo.platform.program;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.zelo.platform.shared.security.CurrentStaff;

@RestController
@RequestMapping("/api/v1/attendances")
public class AttendanceController {

	private final AttendanceService service;

	public AttendanceController(AttendanceService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@PostMapping("/batch")
	@ResponseStatus(HttpStatus.CREATED)
	public AttendanceBatchResponse registerBatch(@Valid @RequestBody AttendanceBatchRequest request,
			Authentication authentication) {
		return service.registerBatch(request, CurrentStaff.from(authentication));
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@PutMapping("/batch")
	public AttendanceBatchResponse correctBatch(@Valid @RequestBody AttendanceBatchRequest request,
			Authentication authentication) {
		return service.correctBatch(request, CurrentStaff.from(authentication));
	}

}
