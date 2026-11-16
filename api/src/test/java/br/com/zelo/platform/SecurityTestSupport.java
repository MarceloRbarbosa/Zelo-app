package br.com.zelo.platform;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Authenticates MockMvc requests with a JWT of the given role, without going through the login endpoint.
public final class SecurityTestSupport {

	private SecurityTestSupport() {
	}

	public static RequestPostProcessor admin() {
		return role("ROLE_ADMIN");
	}

	public static RequestPostProcessor coordinator() {
		return role("ROLE_COORDINATOR");
	}

	public static RequestPostProcessor volunteer() {
		return role("ROLE_VOLUNTEER");
	}

	// For tests that need a real staff id in the token subject: volunteers are scoped to their own activities
	// and some records keep who registered them.
	public static RequestPostProcessor volunteer(long staffId) {
		return member(staffId, "ROLE_VOLUNTEER");
	}

	public static RequestPostProcessor coordinator(long staffId) {
		return member(staffId, "ROLE_COORDINATOR");
	}

	private static RequestPostProcessor member(long staffId, String role) {
		return jwt().jwt(token -> token.subject(String.valueOf(staffId))).authorities(new SimpleGrantedAuthority(role));
	}

	private static RequestPostProcessor role(String role) {
		return jwt().authorities(new SimpleGrantedAuthority(role));
	}

}
