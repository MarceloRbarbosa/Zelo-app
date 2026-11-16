package br.com.zelo.platform.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

// Errors raised by the security chain never reach the @RestControllerAdvice, so they are written here in the
// same ProblemDetail (RFC 9457) shape. Details are fixed texts: nothing from the request is echoed back.
final class ProblemResponses {

	private ProblemResponses() {
	}

	static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType("application/problem+json");
		response.getWriter()
			.write("{\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value() + ",\"detail\":\""
					+ detail + "\"}");
	}

	static AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, exception) -> {
			response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
			write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
		};
	}

	static AccessDeniedHandler accessDeniedHandler() {
		return (request, response, exception) -> write(response, HttpStatus.FORBIDDEN,
				"Access denied for this profile");
	}

}
