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

	private static RequestPostProcessor role(String role) {
		return jwt().authorities(new SimpleGrantedAuthority(role));
	}

}
