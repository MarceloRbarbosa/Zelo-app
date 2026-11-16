package br.com.zelo.platform.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.identity.StaffMemberService;
import br.com.zelo.platform.shared.security.CurrentStaff;

@ExtendWith(MockitoExtension.class)
class TotalsServiceTest {

	private static final CurrentStaff ADMIN = new CurrentStaff("1", false);

	@Mock
	private ActivityRepository activityRepository;

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@Mock
	private BeneficiaryService beneficiaryService;

	@Mock
	private StaffMemberService staffMemberService;

	@InjectMocks
	private TotalsService service;

	@Test
	void countsBeneficiariesActivitiesStudentsAndStaff() {
		when(beneficiaryService.countActive()).thenReturn(12L);
		when(staffMemberService.countActive()).thenReturn(3L);
		when(activityRepository.findAllByStatusOrderByTitleAsc(ActivityStatus.ACTIVE))
			.thenReturn(List.of(activity(1L), activity(2L)));
		Enrollment left = new Enrollment(2L, 11L);
		left.leave(LocalDate.of(2026, 11, 3));
		when(enrollmentRepository.findAllByActivityIdIn(List.of(1L, 2L)))
			.thenReturn(List.of(new Enrollment(1L, 10L), new Enrollment(2L, 10L), left));
		when(beneficiaryService.findActiveIds(List.of(10L))).thenReturn(Set.of(10L));

		assertThat(service.get(ADMIN)).isEqualTo(new TotalsResponse(12, 2, 2, 3L));
	}

	@Test
	void leavesOutInactiveBeneficiariesFromStudents() {
		when(activityRepository.findAllByStatusOrderByTitleAsc(ActivityStatus.ACTIVE)).thenReturn(List.of(activity(1L)));
		when(enrollmentRepository.findAllByActivityIdIn(List.of(1L))).thenReturn(List.of(new Enrollment(1L, 10L)));
		when(beneficiaryService.findActiveIds(List.of(10L))).thenReturn(Set.of());

		assertThat(service.get(ADMIN).activeEnrollments()).isZero();
	}

	@Test
	void doesNotLookForStudentsWithoutActivities() {
		when(activityRepository.findAllByStatusOrderByTitleAsc(ActivityStatus.ACTIVE)).thenReturn(List.of());

		assertThat(service.get(ADMIN).activeActivities()).isZero();
		verifyNoInteractions(enrollmentRepository);
	}

	@Test
	void volunteersCountOnlyTheirActivitiesAndDoNotSeeTheTeam() {
		when(activityRepository.findAllByStatusAndStaffId(ActivityStatus.ACTIVE, 7L)).thenReturn(List.of());

		TotalsResponse totals = service.get(new CurrentStaff("7", true));

		assertThat(totals.activeStaff()).isNull();
		verifyNoInteractions(staffMemberService);
	}

	private static Activity activity(Long id) {
		Activity activity = new Activity("Activity " + id, null, LocalDate.of(2026, 10, 1), List.of(), List.of());
		ReflectionTestUtils.setField(activity, "id", id);
		return activity;
	}

}
