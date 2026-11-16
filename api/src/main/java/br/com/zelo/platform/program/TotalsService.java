package br.com.zelo.platform.program;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.identity.StaffMemberService;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class TotalsService {

	private final ActivityRepository activityRepository;

	private final EnrollmentRepository enrollmentRepository;

	private final BeneficiaryService beneficiaryService;

	private final StaffMemberService staffMemberService;

	public TotalsService(ActivityRepository activityRepository, EnrollmentRepository enrollmentRepository,
			BeneficiaryService beneficiaryService, StaffMemberService staffMemberService) {
		this.activityRepository = activityRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.beneficiaryService = beneficiaryService;
		this.staffMemberService = staffMemberService;
	}

	// Counts instead of lists, so the home page knows what exists without downloading every record.
	// Volunteers count only the activities they are responsible for.
	@Transactional(readOnly = true)
	public TotalsResponse get(CurrentStaff requester) {
		List<Long> activityIds = (requester.volunteer()
				? activityRepository.findAllByStatusAndStaffId(ActivityStatus.ACTIVE, requester.id())
				: activityRepository.findAllByStatusOrderByTitleAsc(ActivityStatus.ACTIVE))
			.stream()
			.map(Activity::getId)
			.toList();
		return new TotalsResponse(beneficiaryService.countActive(), activityIds.size(), students(activityIds),
				requester.volunteer() ? null : staffMemberService.countActive());
	}

	// Active enrollments of beneficiaries who are still active, as in the diary and the alerts.
	private long students(List<Long> activityIds) {
		if (activityIds.isEmpty()) {
			return 0;
		}
		List<Enrollment> enrollments = enrollmentRepository.findAllByActivityIdIn(activityIds)
			.stream()
			.filter(Enrollment::isActive)
			.toList();
		if (enrollments.isEmpty()) {
			return 0;
		}
		Set<Long> active = beneficiaryService
			.findActiveIds(enrollments.stream().map(Enrollment::getBeneficiaryId).distinct().toList());
		return enrollments.stream().filter(enrollment -> active.contains(enrollment.getBeneficiaryId())).count();
	}

}
