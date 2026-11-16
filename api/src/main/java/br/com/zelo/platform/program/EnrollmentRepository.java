package br.com.zelo.platform.program;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	Optional<Enrollment> findByActivityIdAndBeneficiaryId(Long activityId, Long beneficiaryId);

	List<Enrollment> findAllByActivityId(Long activityId);

	List<Enrollment> findAllByActivityIdIn(Collection<Long> activityIds);

	@Query("select e.beneficiaryId from Enrollment e where e.activityId = :activityId and e.status = :status")
	List<Long> findBeneficiaryIds(Long activityId, EnrollmentStatus status);

}
