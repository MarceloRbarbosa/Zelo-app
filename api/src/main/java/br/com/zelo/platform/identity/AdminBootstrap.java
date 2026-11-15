package br.com.zelo.platform.identity;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Creates the first administrator from ADMIN_EMAIL / ADMIN_PASSWORD when none exists yet.
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final StaffMemberRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final String email;

	private final String password;

	public AdminBootstrap(StaffMemberRepository repository, PasswordEncoder passwordEncoder,
			@Value("${zelo.security.admin-email}") String email,
			@Value("${zelo.security.admin-password}") String password) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (repository.existsByRole(StaffRole.ROLE_ADMIN)) {
			return;
		}
		if (email.isBlank() || password.isBlank()) {
			log.warn("No administrator exists and ADMIN_EMAIL/ADMIN_PASSWORD are not set; nobody can log in");
			return;
		}
		repository.save(new StaffMember("Administrator", email.toLowerCase(Locale.ROOT), passwordEncoder.encode(password),
				StaffMemberType.PROFESSIONAL, StaffRole.ROLE_ADMIN));
		log.info("Administrator {} created", email);
	}

}
