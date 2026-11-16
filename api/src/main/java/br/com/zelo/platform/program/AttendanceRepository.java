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

	List<Attendance> findAllByActivityIdAndAttendanceDateAndBeneficiaryIdIn(Long activityId, LocalDate attendanceDate,
			Collection<Long> beneficiaryIds);

	List<Attendance> findAllByActivityIdAndAttendanceDateBetween(Long activityId, LocalDate from, LocalDate to);

	@Query("""
			select a.beneficiaryId as beneficiaryId, count(a) as total,
			       sum(case when a.present = true then 1 else 0 end) as present
			from Attendance a
			where a.activityId = :activityId
			group by a.beneficiaryId
			""")
	List<AttendanceTotals> findTotals(Long activityId);

	@Query("""
			select a.activityId as activityId, count(a) as total,
			       sum(case when a.present = true then 1 else 0 end) as present
			from Attendance a
			where a.activityId in :activityIds
			  and a.attendanceDate between :from and :to
			group by a.activityId
			""")
	List<RollCallTotals> findRollCallTotals(Collection<Long> activityIds, LocalDate from, LocalDate to);

	// Counts only the absences after the last presence, i.e. the current streak. Only beneficiaries still
	// enrolled count; staffId, when given, limits the result to the activities that member is responsible for.
	@Query(nativeQuery = true, value = """
			SELECT a.activity_id          AS "activityId",
			       a.beneficiary_id       AS "beneficiaryId",
			       COUNT(*)               AS "consecutiveAbsences",
			       MAX(a.attendance_date) AS "lastAttendanceDate"
			FROM attendances a
			JOIN activities act ON act.id = a.activity_id AND act.status = 'ACTIVE'
			JOIN enrollments e ON e.activity_id = a.activity_id AND e.beneficiary_id = a.beneficiary_id
			                  AND e.status = 'ACTIVE'
			WHERE a.is_present = false
			  AND (CAST(:activityId AS BIGINT) IS NULL OR a.activity_id = :activityId)
			  AND (CAST(:staffId AS BIGINT) IS NULL OR EXISTS (
			        SELECT 1 FROM activity_staff s WHERE s.activity_id = a.activity_id AND s.staff_id = :staffId))
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
	List<AbsenceStreak> findCurrentAbsenceStreaks(Long activityId, Long staffId, int threshold);

}
