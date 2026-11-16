package br.com.zelo.platform.program;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;

	private final ActivityService activityService;

	private final EnrollmentRepository enrollmentRepository;

	private final BeneficiaryService beneficiaryService;

	public AttendanceService(AttendanceRepository attendanceRepository, ActivityService activityService,
			EnrollmentRepository enrollmentRepository, BeneficiaryService beneficiaryService) {
		this.attendanceRepository = attendanceRepository;
		this.activityService = activityService;
		this.enrollmentRepository = enrollmentRepository;
		this.beneficiaryService = beneficiaryService;
	}

	@Transactional
	public AttendanceBatchResponse registerBatch(AttendanceBatchRequest request, CurrentStaff requester) {
		Activity activity = validate(request, requester);

		List<Long> alreadyRegistered = attendanceRepository.findRegisteredBeneficiaryIds(activity.getId(),
				request.attendanceDate(), beneficiaryIds(request));
		if (!alreadyRegistered.isEmpty()) {
			throw new ConflictException("Attendance already registered on " + request.attendanceDate()
					+ " for beneficiaries: " + new TreeSet<>(alreadyRegistered));
		}

		List<Attendance> attendances = request.entries()
			.stream()
			.map(entry -> new Attendance(activity.getId(), entry.beneficiaryId(), request.attendanceDate(),
					entry.present(), entry.notes()))
			.toList();
		return save(activity, request, attendances);
	}

	// Corrects the marks already registered on the date and registers the ones still missing.
	@Transactional
	public AttendanceBatchResponse correctBatch(AttendanceBatchRequest request, CurrentStaff requester) {
		Activity activity = validate(request, requester);

		Map<Long, Attendance> existing = attendanceRepository
			.findAllByActivityIdAndAttendanceDateAndBeneficiaryIdIn(activity.getId(), request.attendanceDate(),
					beneficiaryIds(request))
			.stream()
			.collect(Collectors.toMap(Attendance::getBeneficiaryId, Function.identity()));
		List<Attendance> attendances = request.entries().stream().map(entry -> {
			Attendance attendance = existing.get(entry.beneficiaryId());
			if (attendance == null) {
				return new Attendance(activity.getId(), entry.beneficiaryId(), request.attendanceDate(),
						entry.present(), entry.notes());
			}
			attendance.correct(entry.present(), entry.notes());
			return attendance;
		}).toList();
		return save(activity, request, attendances);
	}

	private Activity validate(AttendanceBatchRequest request, CurrentStaff requester) {
		Activity activity = activityService.findAccessible(request.activityId(), requester);
		if (!activity.isActive()) {
			throw new ConflictException("Activity " + activity.getId() + " is finished");
		}

		Set<Long> beneficiaryIds = new TreeSet<>();
		for (AttendanceBatchRequest.Entry entry : request.entries()) {
			if (!beneficiaryIds.add(entry.beneficiaryId())) {
				throw new InvalidRequestException("Beneficiary " + entry.beneficiaryId() + " appears more than once");
			}
		}

		Set<Long> notActive = new TreeSet<>(beneficiaryIds);
		notActive.removeAll(beneficiaryService.findActiveIds(beneficiaryIds));
		if (!notActive.isEmpty()) {
			throw new InvalidRequestException("Beneficiaries not found or inactive: " + notActive);
		}

		Set<Long> notEnrolled = new TreeSet<>(beneficiaryIds);
		notEnrolled.removeAll(enrollmentRepository.findBeneficiaryIds(activity.getId(), EnrollmentStatus.ACTIVE));
		if (!notEnrolled.isEmpty()) {
			throw new InvalidRequestException("Beneficiaries not enrolled in the activity: " + notEnrolled);
		}
		return activity;
	}

	private AttendanceBatchResponse save(Activity activity, AttendanceBatchRequest request,
			List<Attendance> attendances) {
		attendanceRepository.saveAllAndFlush(attendances);
		int present = (int) attendances.stream().filter(Attendance::isPresent).count();
		return new AttendanceBatchResponse(activity.getId(), request.attendanceDate(), attendances.size(), present,
				attendances.size() - present);
	}

	private static List<Long> beneficiaryIds(AttendanceBatchRequest request) {
		return request.entries().stream().map(AttendanceBatchRequest.Entry::beneficiaryId).toList();
	}

}
