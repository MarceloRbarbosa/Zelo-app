package br.com.zelo.platform.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@ExtendWith(MockitoExtension.class)
class ProgramOverviewServiceTest {

	private static final CurrentStaff ADMIN = new CurrentStaff("1", false);

	// Monday to Sunday
	private static final LocalDate FROM = LocalDate.of(2026, 11, 2);

	private static final LocalDate TO = LocalDate.of(2026, 11, 8);

	@Mock
	private ActivityRepository activityRepository;

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@Mock
	private AttendanceRepository attendanceRepository;

	@Mock
	private BeneficiaryService beneficiaryService;

	@InjectMocks
	private ProgramOverviewService service;

	@Test
	void rejectsInvertedPeriod() {
		assertThatThrownBy(() -> service.get(TO, FROM, ADMIN)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void returnsEmptyWithoutQueryingWhenNoActivityRunsInThePeriod() {
		when(activityRepository.findAllOverlapping(FROM, TO)).thenReturn(List.of());

		ProgramOverviewResponse overview = service.get(FROM, TO, ADMIN);

		assertThat(overview.attendanceRate()).isNull();
		assertThat(overview.activities()).isEmpty();
		verifyNoInteractions(attendanceRepository, enrollmentRepository, beneficiaryService);
	}

	@Test
	void combinesAttendanceRateStudentsAndPendingRollCalls() {
		// Reading meets on Monday and Wednesday; only Monday has a roll call.
		Activity reading = activity(1L, "Reading", Set.of(), DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
		when(activityRepository.findAllOverlapping(FROM, TO)).thenReturn(List.of(reading));
		when(attendanceRepository.findRollCallTotals(List.of(1L), FROM, TO))
			.thenReturn(List.of(rollCall(1L, FROM, 4, 3)));
		when(enrollmentRepository.findAllByActivityIdIn(List.of(1L)))
			.thenReturn(List.of(enrollment(1L, 10L), enrollment(1L, 11L)));
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(contact(10L), contact(11L)));

		ProgramOverviewResponse overview = service.get(FROM, TO, ADMIN);

		assertThat(overview.attendanceRate()).isEqualTo(75);
		assertThat(overview.activeEnrollments()).isEqualTo(2);
		assertThat(overview.activities())
			.containsExactly(new ProgramOverviewResponse.ActivityOverview(1L, "Reading", ActivityStatus.ACTIVE, 75, 2));
		assertThat(overview.pendingRollCalls())
			.containsExactly(new ProgramOverviewResponse.PendingRollCall(1L, "Reading", LocalDate.of(2026, 11, 4)));
	}

	@Test
	void ratesEachActivityAndTheWholePeriodSeparately() {
		Activity music = activity(1L, "Music", Set.of(), DayOfWeek.MONDAY);
		Activity art = activity(2L, "Art", Set.of(), DayOfWeek.MONDAY);
		when(activityRepository.findAllOverlapping(FROM, TO)).thenReturn(List.of(music, art));
		when(attendanceRepository.findRollCallTotals(any(), any(), any()))
			.thenReturn(List.of(rollCall(1L, FROM, 2, 2), rollCall(2L, FROM, 2, 0)));
		when(enrollmentRepository.findAllByActivityIdIn(any())).thenReturn(List.of());

		ProgramOverviewResponse overview = service.get(FROM, TO, ADMIN);

		assertThat(overview.attendanceRate()).isEqualTo(50);
		assertThat(overview.activities()).extracting(ProgramOverviewResponse.ActivityOverview::title,
				ProgramOverviewResponse.ActivityOverview::attendanceRate)
			.containsExactly(tuple("Art", 0), tuple("Music", 100));
	}

	@Test
	void countsOnlyActiveBeneficiariesAndSkipsActivitiesWithoutStudents() {
		Activity reading = activity(1L, "Reading", Set.of(), DayOfWeek.MONDAY);
		when(activityRepository.findAllOverlapping(FROM, TO)).thenReturn(List.of(reading));
		when(attendanceRepository.findRollCallTotals(any(), any(), any())).thenReturn(List.of());
		// Beneficiary 10 was deactivated but is still enrolled.
		when(enrollmentRepository.findAllByActivityIdIn(any())).thenReturn(List.of(enrollment(1L, 10L)));
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of());

		ProgramOverviewResponse overview = service.get(FROM, TO, ADMIN);

		assertThat(overview.activeEnrollments()).isZero();
		assertThat(overview.pendingRollCalls()).isEmpty();
	}

	@Test
	void finishedActivitiesHaveNoStudentsNorPendingRollCalls() {
		Activity chess = activity(1L, "Chess", Set.of(), DayOfWeek.MONDAY);
		chess.finish(TO);
		when(activityRepository.findAllOverlapping(FROM, TO)).thenReturn(List.of(chess));
		when(attendanceRepository.findRollCallTotals(any(), any(), any())).thenReturn(List.of());
		when(enrollmentRepository.findAllByActivityIdIn(any())).thenReturn(List.of(enrollment(1L, 10L)));
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(contact(10L)));

		ProgramOverviewResponse overview = service.get(FROM, TO, ADMIN);

		assertThat(overview.activeEnrollments()).isZero();
		assertThat(overview.pendingRollCalls()).isEmpty();
	}

	@Test
	void countsDeparturesWithinThePeriodOnly() {
		Activity reading = activity(1L, "Reading", Set.of(), DayOfWeek.MONDAY);
		Enrollment leftInPeriod = enrollment(1L, 10L);
		leftInPeriod.leave(LocalDate.of(2026, 11, 5));
		Enrollment leftBefore = enrollment(1L, 11L);
		leftBefore.leave(LocalDate.of(2026, 10, 30));
		when(activityRepository.findAllOverlapping(FROM, TO)).thenReturn(List.of(reading));
		when(attendanceRepository.findRollCallTotals(any(), any(), any())).thenReturn(List.of());
		when(enrollmentRepository.findAllByActivityIdIn(any())).thenReturn(List.of(leftInPeriod, leftBefore));
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(contact(10L), contact(11L)));

		assertThat(service.get(FROM, TO, ADMIN).leftEnrollments()).isEqualTo(1);
	}

	@Test
	void limitsVolunteersToTheirOwnActivities() {
		Activity music = activity(1L, "Music", Set.of(7L), DayOfWeek.MONDAY);
		Activity art = activity(2L, "Art", Set.of(8L), DayOfWeek.MONDAY);
		when(activityRepository.findAllOverlapping(FROM, TO)).thenReturn(List.of(music, art));
		when(attendanceRepository.findRollCallTotals(List.of(1L), FROM, TO)).thenReturn(List.of());
		when(enrollmentRepository.findAllByActivityIdIn(List.of(1L))).thenReturn(List.of());

		ProgramOverviewResponse overview = service.get(FROM, TO, new CurrentStaff("7", true));

		assertThat(overview.activities()).extracting(ProgramOverviewResponse.ActivityOverview::title)
			.containsExactly("Music");
	}

	private static Activity activity(Long id, String title, Set<Long> staffIds, DayOfWeek... days) {
		List<ActivitySchedule> schedules = Arrays.stream(days)
			.map(day -> new ActivitySchedule(day, LocalTime.of(14, 0), LocalTime.of(15, 0)))
			.toList();
		Activity activity = new Activity(title, null, LocalDate.of(2026, 10, 1), schedules, staffIds);
		ReflectionTestUtils.setField(activity, "id", id);
		return activity;
	}

	private static Enrollment enrollment(Long activityId, Long beneficiaryId) {
		return new Enrollment(activityId, beneficiaryId);
	}

	private static BeneficiaryContact contact(Long id) {
		return new BeneficiaryContact(id, "Student " + id, "22900000000");
	}

	private static RollCallTotals rollCall(Long activityId, LocalDate date, long total, long present) {
		return new RollCallTotals() {

			@Override
			public Long getActivityId() {
				return activityId;
			}

			@Override
			public LocalDate getAttendanceDate() {
				return date;
			}

			@Override
			public Long getTotal() {
				return total;
			}

			@Override
			public Long getPresent() {
				return present;
			}

		};
	}

}
