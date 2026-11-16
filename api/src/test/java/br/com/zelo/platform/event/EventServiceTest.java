package br.com.zelo.platform.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.util.ReflectionTestUtils;

import br.com.zelo.platform.identity.StaffMemberService;
import br.com.zelo.platform.identity.StaffSummary;
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.ForbiddenException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	private static final Instant DATE = Instant.parse("2026-12-05T13:00:00Z");

	@Mock
	private EventRepository repository;

	@Mock
	private StaffMemberService staffMemberService;

	@InjectMocks
	private EventService service;

	@Test
	void createSavesEventAsScheduledWithStaff() {
		when(staffMemberService.requireExisting(Set.of(7L))).thenReturn(Set.of(7L));
		when(staffMemberService.findSummaries(any())).thenReturn(List.of(new StaffSummary(7L, "Vera")));
		when(repository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EventResponse response = service.create(new EventRequest("Feira de saúde", null, DATE, "Quadra", Set.of(7L)));

		assertThat(response.status()).isEqualTo(EventStatus.SCHEDULED);
		assertThat(response.staff()).containsExactly(new StaffSummary(7L, "Vera"));
	}

	@Test
	void onlyScheduledEventsChangeStatusOrData() {
		Event event = event(Set.of());
		event.cancel();
		when(repository.findById(1L)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> service.complete(1L)).isInstanceOf(ConflictException.class);
		assertThatThrownBy(() -> service.update(1L, new EventRequest("x", null, DATE, null, Set.of(7L))))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void listRejectsInvertedPeriod() {
		assertThatThrownBy(() -> service.list(DATE, DATE.minusSeconds(1))).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void participantsAreReachedOnlyByResponsibleVolunteers() {
		when(repository.findById(1L)).thenReturn(Optional.of(event(Set.of(7L))));

		assertThat(service.findForParticipants(1L, new CurrentStaff("7", true))).isNotNull();
		assertThat(service.findForParticipants(1L, new CurrentStaff("8", false))).isNotNull();
		assertThatThrownBy(() -> service.findForParticipants(1L, new CurrentStaff("8", true)))
			.isInstanceOf(ForbiddenException.class);
	}

	private static Event event(Set<Long> staffIds) {
		Event event = new Event("Feira de saúde", null, DATE, null, staffIds);
		ReflectionTestUtils.setField(event, "id", 1L);
		return event;
	}

}
