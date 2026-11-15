package br.com.zelo.platform.program;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@Service
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;

	private final ActivityRepository activityRepository;

	private final BeneficiaryService beneficiaryService;

	public AttendanceService(AttendanceRepository attendanceRepository, ActivityRepository activityRepository,
			BeneficiaryService beneficiaryService) {
		this.attendanceRepository = attendanceRepository;
		this.activityRepository = activityRepository;
		this.beneficiaryService = beneficiaryService;
	}

	@Transactional
	public AttendanceBatchResponse registerBatch(AttendanceBatchRequest request) {
		Activity activity = activityRepository.findById(request.activityId())
			.orElseThrow(() -> new NotFoundException("Activity " + request.activityId() + " not found"));
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

		List<Long> alreadyRegistered = attendanceRepository.findRegisteredBeneficiaryIds(activity.getId(),
				request.attendanceDate(), beneficiaryIds);
		if (!alreadyRegistered.isEmpty()) {
			throw new ConflictException("Attendance already registered on " + request.attendanceDate()
					+ " for beneficiaries: " + new TreeSet<>(alreadyRegistered));
		}

		List<Attendance> attendances = request.entries()
			.stream()
			.map(entry -> new Attendance(activity.getId(), entry.beneficiaryId(), request.attendanceDate(),
					entry.present(), entry.notes()))
			.toList();
		attendanceRepository.saveAllAndFlush(attendances);

		int present = (int) attendances.stream().filter(Attendance::isPresent).count();
		return new AttendanceBatchResponse(activity.getId(), request.attendanceDate(), attendances.size(), present,
				attendances.size() - present);
	}

}
