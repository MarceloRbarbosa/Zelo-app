package br.com.zelo.platform.program;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendances")
public class AttendanceController {

	private final AttendanceService service;

	public AttendanceController(AttendanceService service) {
		this.service = service;
	}

	@PostMapping("/batch")
	@ResponseStatus(HttpStatus.CREATED)
	public AttendanceBatchResponse registerBatch(@Valid @RequestBody AttendanceBatchRequest request) {
		return service.registerBatch(request);
	}

}
