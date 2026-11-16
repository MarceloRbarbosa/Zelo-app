package br.com.zelo.platform.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

	private static final CurrentStaff ADMIN = new CurrentStaff("1", false);

	private static final BeneficiaryContact ANA = new BeneficiaryContact(10L, "Ana", "22900000001");

	@Mock
	private EventParticipantRepository repository;

	@Mock
	private EventService eventService;

	@Mock
	private BeneficiaryService beneficiaryService;

	@InjectMocks
	private ParticipantService service;

	@Test
	void registerCreatesParticipantNotYetAttended() {
		when(eventService.findForParticipants(1L, ADMIN)).thenReturn(event());
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(ANA));
		when(repository.saveAndFlush(any(EventParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ParticipantResponse response = service.register(1L, new ParticipantRequest(10L), ADMIN);

		assertThat(response.fullName()).isEqualTo("Ana");
		assertThat(response.attended()).isFalse();
	}

	@Test
	void registerRejectsDuplicate() {
		when(eventService.findForParticipants(1L, ADMIN)).thenReturn(event());
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(ANA));
		when(repository.existsByEventIdAndBeneficiaryId(1L, 10L)).thenReturn(true);

		assertThatThrownBy(() -> service.register(1L, new ParticipantRequest(10L), ADMIN))
			.isInstanceOf(ConflictException.class);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void canceledEventAcceptsNoChanges() {
		Event canceled = event();
		canceled.cancel();
		when(eventService.findForParticipants(1L, ADMIN)).thenReturn(canceled);

		assertThatThrownBy(() -> service.register(1L, new ParticipantRequest(10L), ADMIN))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void markAttendanceRecordsWhoCame() {
		EventParticipant participant = new EventParticipant(1L, 10L);
		when(eventService.findForParticipants(1L, ADMIN)).thenReturn(event());
		when(repository.findByEventIdAndBeneficiaryId(1L, 10L)).thenReturn(Optional.of(participant));
		when(beneficiaryService.findActiveContacts(Set.of(10L))).thenReturn(List.of(ANA));

		service.markAttendance(1L, 10L, new AttendanceMarkRequest(true), ADMIN);

		assertThat(participant.isAttended()).isTrue();
	}

	private static Event event() {
		return new Event("Feira de saúde", null, Instant.parse("2026-12-05T13:00:00Z"), null, Set.of(7L));
	}

}
