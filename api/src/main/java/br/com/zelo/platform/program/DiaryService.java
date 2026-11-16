package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class DiaryService {

	static final int MAX_PERIOD_DAYS = 366;

	private final ActivityService activityService;

	private final EnrollmentRepository enrollmentRepository;

	private final AttendanceRepository attendanceRepository;

	private final BeneficiaryService beneficiaryService;

	public DiaryService(ActivityService activityService, EnrollmentRepository enrollmentRepository,
			AttendanceRepository attendanceRepository, BeneficiaryService beneficiaryService) {
		this.activityService = activityService;
		this.enrollmentRepository = enrollmentRepository;
		this.attendanceRepository = attendanceRepository;
		this.beneficiaryService = beneficiaryService;
	}

	@Transactional(readOnly = true)
	public DiaryResponse get(Long activityId, LocalDate from, LocalDate to, CurrentStaff requester) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_PERIOD_DAYS) {
			throw new InvalidRequestException("Period must start before it ends and span at most "
					+ MAX_PERIOD_DAYS + " days");
		}
		Activity activity = activityService.findAccessible(activityId, requester);
		List<Attendance> attendances = attendanceRepository.findAllByActivityIdAndAttendanceDateBetween(activityId,
				from, to);

		List<Enrollment> enrollments = enrollmentRepository.findAllByActivityId(activityId);
		Map<Long, BeneficiaryContact> contacts = beneficiaryService
			.findActiveContacts(enrollments.stream().map(Enrollment::getBeneficiaryId).toList())
			.stream()
			.collect(Collectors.toMap(BeneficiaryContact::id, Function.identity()));
		Map<Long, AttendanceTotals> totals = attendanceRepository.findTotals(activityId)
			.stream()
			.collect(Collectors.toMap(AttendanceTotals::getBeneficiaryId, Function.identity()));
		Set<Long> atRisk = attendanceRepository
			.findCurrentAbsenceStreaks(activityId, null, DropoutAlertService.ABSENCE_THRESHOLD)
			.stream()
			.map(AbsenceStreak::getBeneficiaryId)
			.collect(Collectors.toSet());
		Map<Long, List<DiaryResponse.Mark>> marks = attendances.stream()
			.sorted(Comparator.comparing(Attendance::getAttendanceDate))
			.collect(Collectors.groupingBy(Attendance::getBeneficiaryId, Collectors.mapping(
					attendance -> new DiaryResponse.Mark(attendance.getAttendanceDate(), attendance.isPresent(),
							attendance.getNotes()),
					Collectors.toList())));

		List<DiaryResponse.Student> students = enrollments.stream()
			.filter(enrollment -> contacts.containsKey(enrollment.getBeneficiaryId()))
			.map(enrollment -> {
				Long beneficiaryId = enrollment.getBeneficiaryId();
				return new DiaryResponse.Student(beneficiaryId, contacts.get(beneficiaryId).fullName(),
						enrollment.getStatus(), enrollment.getLeftAt(), rate(totals.get(beneficiaryId)),
						atRisk.contains(beneficiaryId), marks.getOrDefault(beneficiaryId, List.of()));
			})
			.sorted(Comparator.comparing(DiaryResponse.Student::fullName))
			.toList();

		return new DiaryResponse(activity.getId(), activity.getTitle(), activity.getStatus(),
				classDates(activity, attendances, from, to), students);
	}

	// Dates come from the weekly schedules, plus any date that already has attendance (for instance, a class
	// held before the schedule changed).
	private static List<LocalDate> classDates(Activity activity, List<Attendance> attendances, LocalDate from,
			LocalDate to) {
		Set<LocalDate> dates = new TreeSet<>(activity.scheduledDates(from, to));
		attendances.forEach(attendance -> dates.add(attendance.getAttendanceDate()));
		return List.copyOf(dates);
	}

	private static Integer rate(AttendanceTotals totals) {
		if (totals == null || totals.getTotal() == 0) {
			return null;
		}
		return Math.round(100f * totals.getPresent() / totals.getTotal());
	}

}
