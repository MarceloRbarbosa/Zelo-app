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
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

	private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

	@Mock
	private AttendanceRepository attendanceRepository;

	@Mock
	private ActivityRepository activityRepository;

	@Mock
	private BeneficiaryService beneficiaryService;

	@InjectMocks
	private AttendanceService service;

	@Test
	void registerBatchSavesAllEntriesAndCountsPresence() {
		when(activityRepository.findById(1L)).thenReturn(Optional.of(activity(ActivityStatus.ACTIVE)));
		when(beneficiaryService.findActiveIds(any())).thenReturn(Set.of(10L, 11L, 12L));
		when(attendanceRepository.findRegisteredBeneficiaryIds(any(), any(), any())).thenReturn(List.of());

		AttendanceBatchResponse response = service.registerBatch(request(entry(10L, true), entry(11L, false),
				entry(12L, true)));

		assertThat(response.registered()).isEqualTo(3);
		assertThat(response.present()).isEqualTo(2);
		assertThat(response.absent()).isEqualTo(1);
		verify(attendanceRepository).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchThrowsWhenActivityDoesNotExist() {
		when(activityRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true))))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void registerBatchRejectsFinishedActivity() {
		when(activityRepository.findById(1L)).thenReturn(Optional.of(activity(ActivityStatus.FINISHED)));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true))))
			.isInstanceOf(ConflictException.class);
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchRejectsRepeatedBeneficiary() {
		when(activityRepository.findById(1L)).thenReturn(Optional.of(activity(ActivityStatus.ACTIVE)));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true), entry(10L, false))))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageContaining("10");
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchRejectsInactiveOrMissingBeneficiaries() {
		when(activityRepository.findById(1L)).thenReturn(Optional.of(activity(ActivityStatus.ACTIVE)));
		when(beneficiaryService.findActiveIds(any())).thenReturn(Set.of(10L));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true), entry(99L, false))))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageContaining("99");
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void registerBatchRejectsAttendanceAlreadyRegisteredOnDate() {
		when(activityRepository.findById(1L)).thenReturn(Optional.of(activity(ActivityStatus.ACTIVE)));
		when(beneficiaryService.findActiveIds(any())).thenReturn(Set.of(10L));
		when(attendanceRepository.findRegisteredBeneficiaryIds(any(), any(), any())).thenReturn(List.of(10L));

		assertThatThrownBy(() -> service.registerBatch(request(entry(10L, true))))
			.isInstanceOf(ConflictException.class);
		verify(attendanceRepository, never()).saveAllAndFlush(anyList());
	}

	private static Activity activity(ActivityStatus status) {
		Activity activity = new Activity("Reading workshop", null, "WEEKLY_MON_WED", LocalDate.of(2026, 1, 5));
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
