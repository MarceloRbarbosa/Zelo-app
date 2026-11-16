package br.com.zelo.platform.program;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.zelo.platform.shared.security.CurrentStaff;

@RestController
@RequestMapping("/api/v1/dropout-alerts")
public class DropoutAlertController {

	private final DropoutAlertService service;

	public DropoutAlertController(DropoutAlertService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping
	public List<DropoutAlertResponse> list(@RequestParam(required = false) Long activityId,
			Authentication authentication) {
		return service.findAlerts(activityId, CurrentStaff.from(authentication));
	}

}
