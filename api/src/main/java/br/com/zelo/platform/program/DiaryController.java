package br.com.zelo.platform.program;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.zelo.platform.shared.security.CurrentStaff;

@RestController
public class DiaryController {

	private final DiaryService service;

	public DiaryController(DiaryService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping("/api/v1/activities/{activityId}/diary")
	public DiaryResponse get(@PathVariable Long activityId, @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			Authentication authentication) {
		return service.get(activityId, from, to, CurrentStaff.from(authentication));
	}

}
