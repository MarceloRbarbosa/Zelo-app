package br.com.zelo.platform.program;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.zelo.platform.shared.security.CurrentStaff;

@RestController
public class TotalsController {

	private final TotalsService service;

	public TotalsController(TotalsService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping("/api/v1/totals")
	public TotalsResponse get(Authentication authentication) {
		return service.get(CurrentStaff.from(authentication));
	}

}
