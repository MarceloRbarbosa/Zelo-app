package br.com.zelo.platform.program;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

	@Column(name = "recurrence_pattern", length = 50)
	private String recurrencePattern;

	@Column(name = "lead_staff_id")
	private Long leadStaffId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private ActivityStatus status = ActivityStatus.ACTIVE;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	protected Activity() {
	}

	public Activity(String title, String description, String recurrencePattern, LocalDate startDate) {
		this.title = title;
		this.description = description;
		this.recurrencePattern = recurrencePattern;
		this.startDate = startDate;
	}

	public boolean isActive() {
		return status == ActivityStatus.ACTIVE;
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

	public String getRecurrencePattern() {
		return recurrencePattern;
	}

	public Long getLeadStaffId() {
		return leadStaffId;
	}

	public ActivityStatus getStatus() {
		return status;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

}
