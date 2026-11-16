package br.com.zelo.platform.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
class ActivityServiceTest {

	private static final LocalDate START = LocalDate.of(2026, 11, 2);

	private static final ActivityRequest.Schedule MONDAY = new ActivityRequest.Schedule(DayOfWeek.MONDAY,
			LocalTime.of(14, 0), LocalTime.of(15, 30));

	@Mock
	private ActivityRepository repository;

	@Mock
	private StaffMemberService staffMemberService;

	@InjectMocks
	private ActivityService service;

	@Test
	void createSavesSchedulesAndResponsibleStaff() {
		when(staffMemberService.requireExisting(Set.of(7L))).thenReturn(Set.of(7L));
		when(staffMemberService.findSummaries(any())).thenReturn(List.of(new StaffSummary(7L, "Vera")));
		when(repository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ActivityResponse response = service.create(request(List.of(MONDAY), Set.of(7L)));

		assertThat(response.status()).isEqualTo(ActivityStatus.ACTIVE);
		assertThat(response.schedules()).containsExactly(
				new ActivitySchedule(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(15, 30)));
		assertThat(response.staff()).containsExactly(new StaffSummary(7L, "Vera"));
	}

	@Test
	void createRejectsUnknownStaff() {
		when(staffMemberService.requireExisting(Set.of(7L, 99L)))
			.thenThrow(new InvalidRequestException("Staff members not found: [99]"));

		assertThatThrownBy(() -> service.create(request(List.of(MONDAY), Set.of(7L, 99L))))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessage("Staff members not found: [99]");
		verify(repository, never()).save(any());
	}

	@Test
	void createRejectsScheduleEndingBeforeItStarts() {
		ActivityRequest.Schedule backwards = new ActivityRequest.Schedule(DayOfWeek.MONDAY, LocalTime.of(15, 0),
				LocalTime.of(14, 0));

		assertThatThrownBy(() -> service.create(request(List.of(backwards), Set.of(7L))))
			.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void createRejectsRepeatedSchedule() {
		assertThatThrownBy(() -> service.create(request(List.of(MONDAY, MONDAY), Set.of(7L))))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageContaining("repeated");
	}

	@Test
	void finishRecordsEndDate() {
		Activity activity = activity(Set.of());
		when(repository.findById(1L)).thenReturn(Optional.of(activity));

		service.finish(1L, new ActivityFinishRequest(LocalDate.of(2026, 12, 18)));

		assertThat(activity.getStatus()).isEqualTo(ActivityStatus.FINISHED);
		assertThat(activity.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 18));
	}

	@Test
	void finishRejectsEndDateBeforeStart() {
		when(repository.findById(1L)).thenReturn(Optional.of(activity(Set.of())));

		assertThatThrownBy(() -> service.finish(1L, new ActivityFinishRequest(START.minusDays(1))))
			.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void finishedActivityCannotBeEdited() {
		Activity activity = activity(Set.of());
		activity.finish(START);
		when(repository.findById(1L)).thenReturn(Optional.of(activity));

		assertThatThrownBy(() -> service.update(1L, request(List.of(MONDAY), Set.of(7L))))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void volunteerReachesOnlyActivitiesTheyAreResponsibleFor() {
		when(repository.findById(1L)).thenReturn(Optional.of(activity(Set.of(7L))));

		assertThat(service.findAccessible(1L, new CurrentStaff("7", true))).isNotNull();
		assertThatThrownBy(() -> service.findAccessible(1L, new CurrentStaff("8", true)))
			.isInstanceOf(ForbiddenException.class);
		assertThat(service.findAccessible(1L, new CurrentStaff("8", false))).isNotNull();
	}

	@Test
	void volunteerListsOnlyOwnActivities() {
		when(repository.findAllByStatusAndStaffId(ActivityStatus.ACTIVE, 7L)).thenReturn(List.of());

		service.list(ActivityStatus.ACTIVE, new CurrentStaff("7", true));

		verify(repository, never()).findAllByStatusOrderByTitleAsc(any());
	}

	private static ActivityRequest request(List<ActivityRequest.Schedule> schedules, Set<Long> staffIds) {
		return new ActivityRequest("Capoeira", null, START, schedules, staffIds);
	}

	private static Activity activity(Set<Long> staffIds) {
		Activity activity = new Activity("Capoeira", null, START, List.of(), staffIds);
		ReflectionTestUtils.setField(activity, "id", 1L);
		return activity;
	}

}
