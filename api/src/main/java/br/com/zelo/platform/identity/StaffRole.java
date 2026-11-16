package br.com.zelo.platform.identity;

import java.util.Arrays;

import org.springframework.security.core.Authentication;

// Ordered from the top: each role manages only the roles below it, except the admin, who also manages admins.
public enum StaffRole {
	ROLE_ADMIN,
	ROLE_COORDINATOR,
	ROLE_VOLUNTEER;

	public boolean canManage(StaffRole other) {
		return this == ROLE_ADMIN || other.ordinal() > ordinal() && this != ROLE_VOLUNTEER;
	}

	public static StaffRole of(Authentication authentication) {
		return Arrays.stream(values())
			.filter(role -> authentication.getAuthorities()
				.stream()
				.anyMatch(authority -> role.name().equals(authority.getAuthority())))
			.findFirst()
			.orElse(ROLE_VOLUNTEER);
	}

}
