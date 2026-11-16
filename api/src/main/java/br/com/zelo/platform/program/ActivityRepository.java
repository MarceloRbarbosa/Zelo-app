package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

	List<Activity> findAllByStatusOrderByTitleAsc(ActivityStatus status);

	@Query("""
			select a from Activity a join a.staffIds staffId
			where a.status = :status and staffId = :staffId
			order by a.title
			""")
	List<Activity> findAllByStatusAndStaffId(ActivityStatus status, Long staffId);

	@Query("""
			select distinct a from Activity a left join fetch a.schedules
			where a.startDate <= :to and (a.endDate is null or a.endDate >= :from)
			""")
	List<Activity> findAllOverlapping(LocalDate from, LocalDate to);

}
