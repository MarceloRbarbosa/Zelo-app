package br.com.zelo.platform.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.UnauthorizedException;

@Service
public class AuthService {

	static final int MAX_FAILED_ATTEMPTS = 5;

	static final Duration LOCK_DURATION = Duration.ofMinutes(15);

	// One message for every failure, so the response does not reveal which e-mails exist.
	static final String INVALID_CREDENTIALS = "Invalid credentials or account temporarily locked";

	private final StaffMemberRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	private final Clock clock;

	// Compared against when the e-mail is unknown, so the response time does not reveal it either.
	private final String dummyHash;

	public AuthService(StaffMemberRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService,
			Clock clock) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("dummy-password");
	}

	// The failed attempt must be saved even though the method ends with an exception.
	@Transactional(noRollbackFor = UnauthorizedException.class)
	public LoginResponse login(LoginRequest request) {
		Instant now = clock.instant();
		StaffMember staff = repository.findByEmailForUpdate(request.email().toLowerCase(Locale.ROOT)).orElse(null);

		if (staff == null || !staff.isActive()) {
			passwordEncoder.matches(request.password(), dummyHash);
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}
		if (staff.isLocked(now)) {
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}
		if (!passwordEncoder.matches(request.password(), staff.getPasswordHash())) {
			staff.registerFailedLogin(now, MAX_FAILED_ATTEMPTS, LOCK_DURATION);
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}

		staff.registerSuccessfulLogin();
		return tokenService.issue(staff, now);
	}

}
