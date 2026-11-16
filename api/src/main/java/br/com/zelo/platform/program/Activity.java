package br.com.zelo.platform.program;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "activities")
public class Activity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private ActivityStatus status = ActivityStatus.ACTIVE;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@ElementCollection
	@CollectionTable(name = "activity_schedules", joinColumns = @JoinColumn(name = "activity_id"))
	private Set<ActivitySchedule> schedules = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "activity_staff", joinColumns = @JoinColumn(name = "activity_id"))
	@Column(name = "staff_id")
	private Set<Long> staffIds = new HashSet<>();

	protected Activity() {
	}

	public Activity(String title, String description, LocalDate startDate, Collection<ActivitySchedule> schedules,
			Collection<Long> staffIds) {
		update(title, description, startDate, schedules, staffIds);
	}

	public void update(String title, String description, LocalDate startDate, Collection<ActivitySchedule> schedules,
			Collection<Long> staffIds) {
		this.title = title;
		this.description = description;
		this.startDate = startDate;
		this.schedules.clear();
		this.schedules.addAll(schedules);
		this.staffIds.clear();
		this.staffIds.addAll(staffIds);
	}

	public void finish(LocalDate endDate) {
		this.status = ActivityStatus.FINISHED;
		this.endDate = endDate;
	}

	// Dates of the weekly schedules within the period and the activity's lifetime.
	public List<LocalDate> scheduledDates(LocalDate from, LocalDate to) {
		Set<DayOfWeek> days = schedules.stream().map(ActivitySchedule::dayOfWeek).collect(Collectors.toSet());
		LocalDate first = from.isBefore(startDate) ? startDate : from;
		LocalDate last = endDate != null && endDate.isBefore(to) ? endDate : to;
		List<LocalDate> dates = new ArrayList<>();
		for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
			if (days.contains(date.getDayOfWeek())) {
				dates.add(date);
			}
		}
		return dates;
	}

	public boolean isActive() {
		return status == ActivityStatus.ACTIVE;
	}

	public boolean hasStaff(Long staffId) {
		return staffIds.contains(staffId);
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public ActivityStatus getStatus() {
		return status;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public Set<ActivitySchedule> getSchedules() {
		return Set.copyOf(schedules);
	}

	public Set<Long> getStaffIds() {
		return Set.copyOf(staffIds);
	}

}
