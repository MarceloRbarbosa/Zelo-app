package br.com.zelo.platform.program;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dropout-alerts")
public class DropoutAlertController {

	private final DropoutAlertService service;

	public DropoutAlertController(DropoutAlertService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping
	public List<DropoutAlertResponse> list(@RequestParam(required = false) Long activityId) {
		return service.findAlerts(activityId);
	}

}
