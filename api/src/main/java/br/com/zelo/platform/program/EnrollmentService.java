package br.com.zelo.platform.program;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.exception.NotFoundException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class EnrollmentService {

	private final EnrollmentRepository repository;

	private final ActivityService activityService;

	private final BeneficiaryService beneficiaryService;

	public EnrollmentService(EnrollmentRepository repository, ActivityService activityService,
			BeneficiaryService beneficiaryService) {
		this.repository = repository;
		this.activityService = activityService;
		this.beneficiaryService = beneficiaryService;
	}

	// Inactive beneficiaries are left out, as everywhere else in the platform.
	@Transactional(readOnly = true)
	public List<EnrollmentResponse> list(Long activityId, CurrentStaff requester) {
		activityService.findAccessible(activityId, requester);
		List<Enrollment> enrollments = repository.findAllByActivityId(activityId);
		Map<Long, BeneficiaryContact> contacts = beneficiaryService
			.findActiveContacts(enrollments.stream().map(Enrollment::getBeneficiaryId).toList())
			.stream()
			.collect(Collectors.toMap(BeneficiaryContact::id, Function.identity()));
		return enrollments.stream()
			.filter(enrollment -> contacts.containsKey(enrollment.getBeneficiaryId()))
			.map(enrollment -> toResponse(enrollment, contacts.get(enrollment.getBeneficiaryId())))
			.sorted(Comparator.comparing(EnrollmentResponse::fullName))
			.toList();
	}

	// Enrolling someone who left the activity brings the same enrollment back.
	@Transactional
	public EnrollmentResponse enroll(Long activityId, EnrollmentRequest request) {
		activityService.findActive(activityId);
		Long beneficiaryId = request.beneficiaryId();
		BeneficiaryContact contact = activeContact(beneficiaryId);
		Enrollment enrollment = repository.findByActivityIdAndBeneficiaryId(activityId, beneficiaryId)
			.map(existing -> {
				if (existing.isActive()) {
					throw new ConflictException("Beneficiary " + beneficiaryId + " is already enrolled");
				}
				existing.rejoin();
				return existing;
			})
			.orElseGet(() -> repository.save(new Enrollment(activityId, beneficiaryId)));
		return toResponse(enrollment, contact);
	}

	@Transactional
	public EnrollmentResponse changeStatus(Long activityId, Long beneficiaryId, EnrollmentStatusRequest request) {
		activityService.findActive(activityId);
		Enrollment enrollment = repository.findByActivityIdAndBeneficiaryId(activityId, beneficiaryId)
			.orElseThrow(() -> new NotFoundException(
					"Beneficiary " + beneficiaryId + " is not enrolled in activity " + activityId));
		if (request.status() == EnrollmentStatus.LEFT) {
			if (request.leftAt() == null) {
				throw new InvalidRequestException("Leaving date is required");
			}
			enrollment.leave(request.leftAt());
		}
		else {
			enrollment.rejoin();
		}
		return toResponse(enrollment, activeContact(beneficiaryId));
	}

	private BeneficiaryContact activeContact(Long beneficiaryId) {
		return beneficiaryService.findActiveContacts(Set.of(beneficiaryId))
			.stream()
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Beneficiary " + beneficiaryId + " not found"));
	}

	private static EnrollmentResponse toResponse(Enrollment enrollment, BeneficiaryContact contact) {
		return new EnrollmentResponse(enrollment.getBeneficiaryId(), contact.fullName(), enrollment.getStatus(),
				enrollment.getLeftAt());
	}

}
