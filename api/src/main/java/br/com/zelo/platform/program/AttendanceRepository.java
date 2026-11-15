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

	// Counts only the absences after the last presence, i.e. the current streak.
	@Query(nativeQuery = true, value = """
			SELECT a.activity_id          AS "activityId",
			       a.beneficiary_id       AS "beneficiaryId",
			       COUNT(*)               AS "consecutiveAbsences",
			       MAX(a.attendance_date) AS "lastAttendanceDate"
			FROM attendances a
			JOIN activities act ON act.id = a.activity_id AND act.status = 'ACTIVE'
			WHERE a.is_present = false
			  AND (CAST(:activityId AS BIGINT) IS NULL OR a.activity_id = :activityId)
			  AND a.attendance_date > COALESCE(
			        (SELECT MAX(p.attendance_date)
			         FROM attendances p
			         WHERE p.activity_id = a.activity_id
			           AND p.beneficiary_id = a.beneficiary_id
			           AND p.is_present = true),
			        DATE '-infinity')
			GROUP BY a.activity_id, a.beneficiary_id
			HAVING COUNT(*) >= :threshold
			ORDER BY "consecutiveAbsences" DESC, a.activity_id, a.beneficiary_id
			""")
	List<AbsenceStreak> findCurrentAbsenceStreaks(Long activityId, int threshold);

}
