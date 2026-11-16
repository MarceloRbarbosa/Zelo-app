package br.com.zelo.platform.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.exception.NotFoundException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

	private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

	private static final CurrentStaff ADMIN = new CurrentStaff("1", false);

	@Mock
	private AttendanceRepository attendanceRepository;

	@Mock
	private ActivityService activityService;

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@Mock
	private BeneficiaryService beneficiaryService;

	@InjectMocks
	private AttendanceService service;

	@Test
	void registerBatchSavesAllEntriesAndCountsPresence() {
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(ActivityStatus.ACTIVE));
		when(beneficiaryService.findActiveIds(any())).thenReturn(Set.of(10L, 11L, 12L));
		when(enrollmentRepository.findBeneficiaryIds(1L, EnrollmentStatus.ACTIVE)).thenReturn(List.of(10L, 11L, 12L));
		when(attendanceRepository.findRegisteredBeneficiaryIds(any(), any(), any())).thenReturn(List.of());

		AttendanceBatchResponse response = service.registerBatch(request(entry(10L, true), entry(11L, false),
				entry(12L, true)), ADMIN);

		assertThat(response.registered()).isEqualTo(3);
		assertThat(response.present()).isEqualTo(2);
		assertThat(response.absent()).isEqualTo(1);
		verify(attendanceRepository).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchThrowsWhenActivityDoesNotExist() {
		when(activityService.findAccessible(1L, ADMIN)).thenThrow(new NotFoundException("Activity 1 not found"));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true)), ADMIN))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void registerBatchRejectsFinishedActivity() {
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(ActivityStatus.FINISHED));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true)), ADMIN))
			.isInstanceOf(ConflictException.class);
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchRejectsRepeatedBeneficiary() {
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(ActivityStatus.ACTIVE));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true), entry(10L, false)), ADMIN))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageContaining("10");
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchRejectsInactiveOrMissingBeneficiaries() {
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(ActivityStatus.ACTIVE));
		when(beneficiaryService.findActiveIds(any())).thenReturn(Set.of(10L));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true), entry(99L, false)), ADMIN))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageContaining("99");
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchRejectsBeneficiariesNotEnrolledInTheActivity() {
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(ActivityStatus.ACTIVE));
		when(beneficiaryService.findActiveIds(any())).thenReturn(Set.of(10L, 11L));
		when(enrollmentRepository.findBeneficiaryIds(1L, EnrollmentStatus.ACTIVE)).thenReturn(List.of(10L));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true), entry(11L, false)), ADMIN))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessage("Beneficiaries not enrolled in the activity: [11]");
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchRejectsAttendanceAlreadyRegisteredOnDate() {
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(ActivityStatus.ACTIVE));
		when(beneficiaryService.findActiveIds(any())).thenReturn(Set.of(10L));
		when(enrollmentRepository.findBeneficiaryIds(1L, EnrollmentStatus.ACTIVE)).thenReturn(List.of(10L));
		when(attendanceRepository.findRegisteredBeneficiaryIds(any(), any(), any())).thenReturn(List.of(10L));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true)), ADMIN))
			.isInstanceOf(ConflictException.class);
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void correctBatchUpdatesExistingAttendanceAndCreatesMissingOne() {
		Attendance existing = new Attendance(1L, 10L, DATE, true, null);
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(ActivityStatus.ACTIVE));
		when(beneficiaryService.findActiveIds(any())).thenReturn(Set.of(10L, 11L));
		when(enrollmentRepository.findBeneficiaryIds(1L, EnrollmentStatus.ACTIVE)).thenReturn(List.of(10L, 11L));
		when(attendanceRepository.findAllByActivityIdAndAttendanceDateAndBeneficiaryIdIn(any(), any(), any()))
			.thenReturn(List.of(existing));

		AttendanceBatchResponse response = service.correctBatch(
				request(new AttendanceBatchRequest.Entry(10L, false, "Chegou atrasado"), entry(11L, true)), ADMIN);

		assertThat(existing.isPresent()).isFalse();
		assertThat(existing.getNotes()).isEqualTo("Chegou atrasado");
		assertThat(response.registered()).isEqualTo(2);
		assertThat(response.present()).isEqualTo(1);
		verify(attendanceRepository).saveAllAndFlush(anyList());
	}

	@Test
	void correctBatchRejectsFinishedActivity() {
		when(activityService.findAccessible(1L, ADMIN)).thenReturn(activity(ActivityStatus.FINISHED));

		assertThatThrownBy(() -> service.correctBatch(request(entry(10L, true)), ADMIN))
			.isInstanceOf(ConflictException.class);
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	private static Activity activity(ActivityStatus status) {
		Activity activity = new Activity("Reading workshop", null, LocalDate.of(2026, 1, 5), List.of(), List.of());
		ReflectionTestUtils.setField(activity, "id", 1L);
		ReflectionTestUtils.setField(activity, "status", status);
		return activity;
	}

	private static AttendanceBatchRequest request(AttendanceBatchRequest.Entry... entries) {
		return new AttendanceBatchRequest(1L, DATE, List.of(entries));
	}

	private static AttendanceBatchRequest.Entry entry(Long beneficiaryId, boolean present) {
		return new AttendanceBatchRequest.Entry(beneficiaryId, present, null);
	}

}
