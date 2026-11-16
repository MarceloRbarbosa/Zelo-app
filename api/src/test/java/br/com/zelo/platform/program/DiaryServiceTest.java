package br.com.zelo.platform.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
class DiaryServiceTest {

	private static final CurrentStaff ADMIN = new CurrentStaff("1", false);

	// Monday 2026-11-02
	private static final LocalDate START = LocalDate.of(2026, 11, 2);

	@Mock
	private ActivityService activityService;

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@Mock
	private AttendanceRepository attendanceRepository;

	@Mock
	private BeneficiaryService beneficiaryService;

	@InjectMocks
	private DiaryService service;

	@Test
	void datesFollowWeeklySchedulesWithinTheActivityLifetime() {
		Activity activity = activity(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity);

		// The period starts before the activity, so only dates from 2026-11-02 on count.
		DiaryResponse diary = service.get(1L, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 11), ADMIN);

		assertThat(diary.dates()).containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4),
				LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 11));
	}

	@Test
	void datesStopAtTheEndDateAndIncludeOffScheduleAttendance() {
		Activity activity = activity(DayOfWeek.MONDAY);
		activity.finish(LocalDate.of(2026, 11, 10));
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity);
		// A class held on a Friday, outside the schedule
		when(attendanceRepository.findAllByActivityIdAndAttendanceDateBetween(any(), any(), any()))
			.thenReturn(List.of(new Attendance(1L, 10L, LocalDate.of(2026, 11, 6), true, null)));

		DiaryResponse diary = service.get(1L, START, LocalDate.of(2026, 11, 30), ADMIN);

		assertThat(diary.dates()).containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6),
				LocalDate.of(2026, 11, 9));
	}

	@Test
	void studentsCarryRateRiskAndMarks() {
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(DayOfWeek.MONDAY));
		when(enrollmentRepository.findAllByActivityId(1L)).thenReturn(List.of(new Enrollment(1L, 10L),
				new Enrollment(1L, 11L)));
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of(
				new BeneficiaryContact(10L, "Bruno", "22900000002"), new BeneficiaryContact(11L, "Ana", "22900000001")));
		when(attendanceRepository.findAllByActivityIdAndAttendanceDateBetween(any(), any(), any()))
			.thenReturn(List.of(new Attendance(1L, 10L, START, false, "Doente")));
		when(attendanceRepository.findTotals(1L)).thenReturn(List.of(totals(10L, 4, 1)));
		when(attendanceRepository.findCurrentAbsenceStreaks(1L, null, 3)).thenReturn(List.of(streak(10L)));

		DiaryResponse diary = service.get(1L, START, START, ADMIN);

		assertThat(diary.students()).extracting(DiaryResponse.Student::fullName).containsExactly("Ana", "Bruno");
		DiaryResponse.Student ana = diary.students().get(0);
		assertThat(ana.attendanceRate()).isNull();
		assertThat(ana.dropoutRisk()).isFalse();
		DiaryResponse.Student bruno = diary.students().get(1);
		assertThat(bruno.attendanceRate()).isEqualTo(25);
		assertThat(bruno.dropoutRisk()).isTrue();
		assertThat(bruno.marks()).containsExactly(new DiaryResponse.Mark(START, false, "Doente"));
	}

	@Test
	void rejectsInvertedOrTooLongPeriods() {
		assertThatThrownBy(() -> service.get(1L, START, START.minusDays(1), ADMIN))
			.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> service.get(1L, START, START.plusDays(366), ADMIN))
			.isInstanceOf(InvalidRequestException.class);
	}

	private static Activity activity(DayOfWeek... days) {
		List<ActivitySchedule> schedules = Arrays.stream(days)
			.map(day -> new ActivitySchedule(day, LocalTime.of(14, 0), LocalTime.of(15, 0)))
			.toList();
		Activity activity = new Activity("Capoeira", null, START, schedules, Set.of());
		ReflectionTestUtils.setField(activity, "id", 1L);
		return activity;
	}

	private static AttendanceTotals totals(Long beneficiaryId, long total, long present) {
		return new AttendanceTotals() {

			@Override
			public Long getBeneficiaryId() {
				return beneficiaryId;
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

	private static AbsenceStreak streak(Long beneficiaryId) {
		return new AbsenceStreak() {

			@Override
			public Long getActivityId() {
				return 1L;
			}

			@Override
			public Long getBeneficiaryId() {
				return beneficiaryId;
			}

			@Override
			public Long getConsecutiveAbsences() {
				return 3L;
			}

			@Override
			public LocalDate getLastAttendanceDate() {
				return START;
			}

		};
	}

}
