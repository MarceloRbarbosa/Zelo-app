package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	@Query("""
			select a.beneficiaryId from Attendance a
			where a.activityId = :activityId
			  and a.attendanceDate = :attendanceDate
			  and a.beneficiaryId in :beneficiaryIds
			""")
	List<Long> findRegisteredBeneficiaryIds(Long activityId, LocalDate attendanceDate,
			Collection<Long> beneficiaryIds);

}
