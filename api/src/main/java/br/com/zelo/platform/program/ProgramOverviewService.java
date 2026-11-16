package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class ProgramOverviewService {

	private final ActivityRepository activityRepository;

	private final EnrollmentRepository enrollmentRepository;

	private final AttendanceRepository attendanceRepository;

	private final BeneficiaryService beneficiaryService;

	public ProgramOverviewService(ActivityRepository activityRepository, EnrollmentRepository enrollmentRepository,
			AttendanceRepository attendanceRepository, BeneficiaryService beneficiaryService) {
		this.activityRepository = activityRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.attendanceRepository = attendanceRepository;
		this.beneficiaryService = beneficiaryService;
	}

	// Figures of the activities that run in the period; volunteers only see the activities they are responsible
	// for.
	@Transactional(readOnly = true)
	public ProgramOverviewResponse get(LocalDate from, LocalDate to, CurrentStaff requester) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= DiaryService.MAX_PERIOD_DAYS) {
			throw new InvalidRequestException("Period must start before it ends and span at most "
					+ DiaryService.MAX_PERIOD_DAYS + " days");
		}
		List<Activity> activities = activityRepository.findAllOverlapping(from, to)
			.stream()
			.filter(activity -> !requester.volunteer() || activity.hasStaff(requester.id()))
			.sorted(Comparator.comparing(Activity::getTitle))
			.toList();
		if (activities.isEmpty()) {
			return new ProgramOverviewResponse(from, to, null, 0, 0, List.of());
		}
		List<Long> activityIds = activities.stream().map(Activity::getId).toList();

		List<RollCallTotals> rollCalls = attendanceRepository.findRollCallTotals(activityIds, from, to);
		Map<Long, List<RollCallTotals>> rollCallsByActivity = rollCalls.stream()
			.collect(Collectors.groupingBy(RollCallTotals::getActivityId));

		List<Enrollment> enrollments = enrollmentRepository.findAllByActivityIdIn(activityIds);
		Set<Long> activeBeneficiaries = beneficiaryService
			.findActiveContacts(enrollments.stream().map(Enrollment::getBeneficiaryId).distinct().toList())
			.stream()
			.map(BeneficiaryContact::id)
			.collect(Collectors.toSet());
		// Only active beneficiaries still enrolled in an active activity count as students today.
		Map<Long, Long> activeEnrollments = enrollments.stream()
			.filter(enrollment -> enrollment.isActive() && activeBeneficiaries.contains(enrollment.getBeneficiaryId()))
			.collect(Collectors.groupingBy(Enrollment::getActivityId, Collectors.counting()));
		long left = enrollments.stream()
			.filter(enrollment -> enrollment.getLeftAt() != null && !enrollment.getLeftAt().isBefore(from)
					&& !enrollment.getLeftAt().isAfter(to))
			.count();

		List<ProgramOverviewResponse.ActivityOverview> overviews = activities.stream()
			.map(activity -> new ProgramOverviewResponse.ActivityOverview(activity.getId(), activity.getTitle(),
					activity.getStatus(), rate(rollCallsByActivity.getOrDefault(activity.getId(), List.of())),
					students(activity, activeEnrollments)))
			.toList();

		return new ProgramOverviewResponse(from, to, rate(rollCalls),
				overviews.stream().mapToLong(ProgramOverviewResponse.ActivityOverview::activeEnrollments).sum(), left,
				overviews);
	}

	private static long students(Activity activity, Map<Long, Long> activeEnrollments) {
		return activity.isActive() ? activeEnrollments.getOrDefault(activity.getId(), 0L) : 0;
	}

	private static Integer rate(List<RollCallTotals> rollCalls) {
		long total = rollCalls.stream().mapToLong(RollCallTotals::getTotal).sum();
		if (total == 0) {
			return null;
		}
		long present = rollCalls.stream().mapToLong(RollCallTotals::getPresent).sum();
		return Math.round(100f * present / total);
	}

}
