package br.com.zelo.platform.shared.security;

import org.springframework.security.core.Authentication;

// The member behind a request. Volunteers only reach the activities and events they are responsible for.
public record CurrentStaff(String subject, boolean volunteer) {

	public static CurrentStaff from(Authentication authentication) {
		boolean volunteer = authentication.getAuthorities()
			.stream()
			.anyMatch(authority -> "ROLE_VOLUNTEER".equals(authority.getAuthority()));
		return new CurrentStaff(authentication.getName(), volunteer);
	}

	public Long id() {
		return Long.valueOf(subject);
	}

}
