package br.com.zelo.platform.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;

@ExtendWith(MockitoExtension.class)
class DropoutAlertServiceTest {

	@Mock
	private AttendanceRepository attendanceRepository;

	@Mock
	private ActivityRepository activityRepository;

	@Mock
	private BeneficiaryService beneficiaryService;

	@InjectMocks
	private DropoutAlertService service;

	@Test
	void usesThresholdOfThreeAbsences() {
		when(attendanceRepository.findCurrentAbsenceStreaks(null, 3)).thenReturn(List.of());

		service.findAlerts(null);

		verify(attendanceRepository).findCurrentAbsenceStreaks(null, 3);
	}

	@Test
	void returnsEmptyWithoutQueryingOtherModulesWhenThereAreNoStreaks() {
		when(attendanceRepository.findCurrentAbsenceStreaks(1L, 3)).thenReturn(List.of());

		assertThat(service.findAlerts(1L)).isEmpty();
		verifyNoInteractions(activityRepository, beneficiaryService);
	}

	@Test
	void combinesStreakWithActivityTitleAndBeneficiaryContact() {
		when(attendanceRepository.findCurrentAbsenceStreaks(null, 3)).thenReturn(List.of(streak(1L, 10L, 4)));
		when(activityRepository.findAllById(any())).thenReturn(List.of(activity(1L, "Reading")));
		when(beneficiaryService.findActiveContacts(any()))
			.thenReturn(List.of(new BeneficiaryContact(10L, "Ana", "22900000001")));

		List<DropoutAlertResponse> alerts = service.findAlerts(null);

		assertThat(alerts).containsExactly(new DropoutAlertResponse(1L, "Reading", 10L, "Ana", "22900000001", 4,
				LocalDate.of(2026, 11, 5)));
	}

	@Test
	void leavesOutBeneficiariesWithoutActiveContact() {
		when(attendanceRepository.findCurrentAbsenceStreaks(null, 3))
			.thenReturn(List.of(streak(1L, 10L, 3), streak(1L, 11L, 3)));
		when(activityRepository.findAllById(any())).thenReturn(List.of(activity(1L, "Reading")));
		when(beneficiaryService.findActiveContacts(any()))
			.thenReturn(List.of(new BeneficiaryContact(10L, "Ana", "22900000001")));

		assertThat(service.findAlerts(null)).extracting(DropoutAlertResponse::beneficiaryId).containsExactly(10L);
	}

	private static Activity activity(Long id, String title) {
		Activity activity = new Activity(title, null, null, LocalDate.of(2026, 10, 1));
		ReflectionTestUtils.setField(activity, "id", id);
		return activity;
	}

	private static AbsenceStreak streak(Long activityId, Long beneficiaryId, long absences) {
		return new AbsenceStreak() {

			@Override
			public Long getActivityId() {
				return activityId;
			}

			@Override
			public Long getBeneficiaryId() {
				return beneficiaryId;
			}

			@Override
			public Long getConsecutiveAbsences() {
				return absences;
			}

			@Override
			public LocalDate getLastAttendanceDate() {
				return LocalDate.of(2026, 11, 5);
			}

		};
	}

}
