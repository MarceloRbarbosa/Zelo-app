package br.com.zelo.platform.shared.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

// Checks the member on every request: a blocked account loses access at once instead of when the
// token expires, and a member with a provisional password may only read the profile and change it.
class AccountStatusFilter extends OncePerRequestFilter {

	private final AccountStatusChecker checker;

	AccountStatusFilter(AccountStatusChecker checker) {
		this.checker = checker;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			AccountState state = checker.stateOf(token.getName());
			if (state == AccountState.DISABLED) {
				SecurityContextHolder.clearContext();
				reject(response, HttpStatus.UNAUTHORIZED, "Account blocked or removed");
				return;
			}
			if (state == AccountState.PASSWORD_CHANGE_REQUIRED && !isAllowedBeforePasswordChange(request)) {
				reject(response, HttpStatus.FORBIDDEN, "Password change required");
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private static boolean isAllowedBeforePasswordChange(HttpServletRequest request) {
		String path = request.getRequestURI();
		String method = request.getMethod();
		return (HttpMethod.GET.matches(method) && path.equals("/api/v1/me"))
				|| (HttpMethod.PUT.matches(method) && path.equals("/api/v1/me/password"));
	}

	private static void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType("application/problem+json");
		response.getWriter()
			.write("{\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value() + ",\"detail\":\""
					+ detail + "\"}");
	}

}
