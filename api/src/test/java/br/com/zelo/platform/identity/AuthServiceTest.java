package br.com.zelo.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.zelo.platform.shared.exception.UnauthorizedException;

class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

	private static final String EMAIL = "ana@zelo.org";

	private final StaffMemberRepository repository = mock(StaffMemberRepository.class);

	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

	private final TokenService tokenService = mock(TokenService.class);

	private AuthService service;

	private StaffMember staff;

	@BeforeEach
	void setUp() {
		when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
		service = new AuthService(repository, passwordEncoder, tokenService, Clock.fixed(NOW, ZoneOffset.UTC));
		staff = new StaffMember("Ana", EMAIL, "real-hash", StaffMemberType.COORDINATOR, StaffRole.ROLE_COORDINATOR);
		when(repository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(staff));
	}

	@Test
	void validCredentialsIssueTokenAndResetFailedAttempts() {
		staff.registerFailedLogin(NOW, 5, AuthService.LOCK_DURATION);
		when(passwordEncoder.matches("secret123", "real-hash")).thenReturn(true);
		LoginResponse token = new LoginResponse("token", NOW.plus(TokenService.TOKEN_VALIDITY));
		when(tokenService.issue(staff, NOW)).thenReturn(token);

		assertThat(service.login(new LoginRequest("ANA@zelo.org", "secret123"))).isEqualTo(token);
		assertThat(staff.getFailedLoginAttempts()).isZero();
	}

	@Test
	void wrongPasswordCountsFailedAttempt() {
		assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "wrong")))
			.isInstanceOf(UnauthorizedException.class);

		assertThat(staff.getFailedLoginAttempts()).isEqualTo(1);
		assertThat(staff.getLockedUntil()).isNull();
	}

	@Test
	void fifthWrongPasswordLocksAccountForFifteenMinutes() {
		for (int attempt = 1; attempt <= AuthService.MAX_FAILED_ATTEMPTS; attempt++) {
			assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "wrong")))
				.isInstanceOf(UnauthorizedException.class);
		}

		assertThat(staff.getLockedUntil()).isEqualTo(NOW.plusSeconds(15 * 60));
	}

	@Test
	void lockedAccountIsRejectedEvenWithCorrectPassword() {
		lockAccount();

		assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "secret123")))
			.isInstanceOf(UnauthorizedException.class);
		verify(passwordEncoder, never()).matches("secret123", "real-hash");
	}

	@Test
	void expiredLockAllowsLoginAgain() {
		lockAccount();
		Clock later = Clock.fixed(NOW.plus(AuthService.LOCK_DURATION).plusSeconds(1), ZoneOffset.UTC);
		service = new AuthService(repository, passwordEncoder, tokenService, later);
		when(passwordEncoder.matches("secret123", "real-hash")).thenReturn(true);

		service.login(new LoginRequest(EMAIL, "secret123"));

		verify(tokenService).issue(eq(staff), any());
		assertThat(staff.getLockedUntil()).isNull();
	}

	@Test
	void unknownEmailIsRejectedWithSameMessageAndStillHashesPassword() {
		when(repository.findByEmailForUpdate("nobody@zelo.org")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new LoginRequest("nobody@zelo.org", "secret123")))
			.isInstanceOf(UnauthorizedException.class)
			.hasMessage(AuthService.INVALID_CREDENTIALS);
		verify(passwordEncoder).matches("secret123", "dummy-hash");
	}

	private void lockAccount() {
		for (int attempt = 1; attempt <= AuthService.MAX_FAILED_ATTEMPTS; attempt++) {
			staff.registerFailedLogin(NOW, AuthService.MAX_FAILED_ATTEMPTS, AuthService.LOCK_DURATION);
		}
	}

}
