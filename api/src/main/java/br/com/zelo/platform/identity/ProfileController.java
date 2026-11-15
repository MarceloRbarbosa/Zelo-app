package br.com.zelo.platform.identity;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints for the logged-in member; any authenticated role may use them.
@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {

	private final ProfileService service;

	public ProfileController(ProfileService service) {
		this.service = service;
	}

	@GetMapping
	public ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
		return service.get(staffId(jwt));
	}

	@PutMapping
	public ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileUpdateRequest request) {
		return service.update(staffId(jwt), request);
	}

	@PutMapping("/password")
	public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PasswordChangeRequest request) {
		service.changePassword(staffId(jwt), request);
		return ResponseEntity.noContent().build();
	}

	private static Long staffId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
