package br.com.zelo.platform.program;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;

@Service
public class DropoutAlertService {

	static final int ABSENCE_THRESHOLD = 3;

	private final AttendanceRepository attendanceRepository;

	private final ActivityRepository activityRepository;

	private final BeneficiaryService beneficiaryService;

	public DropoutAlertService(AttendanceRepository attendanceRepository, ActivityRepository activityRepository,
			BeneficiaryService beneficiaryService) {
		this.attendanceRepository = attendanceRepository;
		this.activityRepository = activityRepository;
		this.beneficiaryService = beneficiaryService;
	}

	@Transactional(readOnly = true)
	public List<DropoutAlertResponse> findAlerts(Long activityId) {
		List<AbsenceStreak> streaks = attendanceRepository.findCurrentAbsenceStreaks(activityId, ABSENCE_THRESHOLD);
		if (streaks.isEmpty()) {
			return List.of();
		}

		Map<Long, String> activityTitles = activityRepository
			.findAllById(streaks.stream().map(AbsenceStreak::getActivityId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(Activity::getId, Activity::getTitle));
		Map<Long, BeneficiaryContact> contacts = beneficiaryService
			.findActiveContacts(streaks.stream().map(AbsenceStreak::getBeneficiaryId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(BeneficiaryContact::id, Function.identity()));

		// Inactive beneficiaries have no contact and are left out of the alerts.
		return streaks.stream()
			.filter(streak -> contacts.containsKey(streak.getBeneficiaryId()))
			.map(streak -> {
				BeneficiaryContact contact = contacts.get(streak.getBeneficiaryId());
				return new DropoutAlertResponse(streak.getActivityId(), activityTitles.get(streak.getActivityId()),
						contact.id(), contact.fullName(), contact.phoneNumber(), streak.getConsecutiveAbsences(),
						streak.getLastAttendanceDate());
			})
			.toList();
	}

}
