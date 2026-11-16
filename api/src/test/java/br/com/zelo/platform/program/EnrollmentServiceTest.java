package br.com.zelo.platform.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

	private static final BeneficiaryContact ANA = new BeneficiaryContact(10L, "Ana", "22900000001");

	@Mock
	private EnrollmentRepository repository;

	@Mock
	private ActivityService activityService;

	@Mock
	private BeneficiaryService beneficiaryService;

	@InjectMocks
	private EnrollmentService service;

	@Test
	void enrollCreatesActiveEnrollment() {
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(ANA));
		when(repository.findByActivityIdAndBeneficiaryId(1L, 10L)).thenReturn(Optional.empty());
		when(repository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EnrollmentResponse response = service.enroll(1L, new EnrollmentRequest(10L));

		assertThat(response).isEqualTo(new EnrollmentResponse(10L, "Ana", EnrollmentStatus.ACTIVE, null));
	}

	@Test
	void enrollRejectsUnknownOrInactiveBeneficiary() {
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of());

		assertThatThrownBy(() -> service.enroll(1L, new EnrollmentRequest(10L))).isInstanceOf(NotFoundException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void enrollRejectsBeneficiaryAlreadyEnrolled() {
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(ANA));
		when(repository.findByActivityIdAndBeneficiaryId(1L, 10L)).thenReturn(Optional.of(new Enrollment(1L, 10L)));

		assertThatThrownBy(() -> service.enroll(1L, new EnrollmentRequest(10L))).isInstanceOf(ConflictException.class);
	}

	@Test
	void enrollBringsBackBeneficiaryWhoLeft() {
		Enrollment left = new Enrollment(1L, 10L);
		left.leave(LocalDate.of(2026, 11, 20));
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(ANA));
		when(repository.findByActivityIdAndBeneficiaryId(1L, 10L)).thenReturn(Optional.of(left));

		EnrollmentResponse response = service.enroll(1L, new EnrollmentRequest(10L));

		assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
		assertThat(response.leftAt()).isNull();
		verify(repository, never()).save(any());
	}

	@Test
	void leavingRequiresDate() {
		when(repository.findByActivityIdAndBeneficiaryId(1L, 10L)).thenReturn(Optional.of(new Enrollment(1L, 10L)));

		assertThatThrownBy(
				() -> service.changeStatus(1L, 10L, new EnrollmentStatusRequest(EnrollmentStatus.LEFT, null)))
			.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void leavingKeepsEnrollmentWithDate() {
		Enrollment enrollment = new Enrollment(1L, 10L);
		when(repository.findByActivityIdAndBeneficiaryId(1L, 10L)).thenReturn(Optional.of(enrollment));
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(ANA));

		EnrollmentResponse response = service.changeStatus(1L, 10L,
				new EnrollmentStatusRequest(EnrollmentStatus.LEFT, LocalDate.of(2026, 11, 20)));

		assertThat(response).isEqualTo(new EnrollmentResponse(10L, "Ana", EnrollmentStatus.LEFT,
				LocalDate.of(2026, 11, 20)));
	}

}
