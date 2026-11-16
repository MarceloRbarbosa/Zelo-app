package br.com.zelo.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

	@Mock
	private StaffMemberRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	void createsFirstAdministratorWithNormalizedEmailAndHashedPassword() {
		when(repository.existsByRole(StaffRole.ROLE_ADMIN)).thenReturn(false);
		when(passwordEncoder.encode("troque-esta-senha")).thenReturn("hash");

		bootstrap("Admin@Zelo.Local", "troque-esta-senha").run(null);

		ArgumentCaptor<StaffMember> saved = ArgumentCaptor.forClass(StaffMember.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("admin@zelo.local");
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
		assertThat(saved.getValue().getRole()).isEqualTo(StaffRole.ROLE_ADMIN);
	}

	@Test
	void doesNothingWhenAnAdministratorAlreadyExists() {
		when(repository.existsByRole(StaffRole.ROLE_ADMIN)).thenReturn(true);

		bootstrap("admin@zelo.local", "troque-esta-senha").run(null);

		verify(repository, never()).save(any());
	}

	@Test
	void doesNothingWithoutCredentials() {
		when(repository.existsByRole(StaffRole.ROLE_ADMIN)).thenReturn(false);

		bootstrap("", "").run(null);

		verify(repository, never()).save(any());
	}

	private AdminBootstrap bootstrap(String email, String password) {
		return new AdminBootstrap(repository, passwordEncoder, email, password);
	}

}
