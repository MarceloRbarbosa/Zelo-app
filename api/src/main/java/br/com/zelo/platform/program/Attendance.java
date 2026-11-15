package br.com.zelo.platform.program;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendances")
public class Attendance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "activity_id", nullable = false)
	private Long activityId;

	@Column(name = "beneficiary_id", nullable = false)
	private Long beneficiaryId;

	@Column(name = "attendance_date", nullable = false)
	private LocalDate attendanceDate;

	@Column(name = "is_present", nullable = false)
	private boolean present;

	@Column(name = "notes")
	private String notes;

	protected Attendance() {
	}

	public Attendance(Long activityId, Long beneficiaryId, LocalDate attendanceDate, boolean present,
			String notes) {
		this.activityId = activityId;
		this.beneficiaryId = beneficiaryId;
		this.attendanceDate = attendanceDate;
		this.present = present;
		this.notes = notes;
	}

	public Long getId() {
		return id;
	}

	public Long getActivityId() {
		return activityId;
	}

	public Long getBeneficiaryId() {
		return beneficiaryId;
	}

	public LocalDate getAttendanceDate() {
		return attendanceDate;
	}

	public boolean isPresent() {
		return present;
	}

	public String getNotes() {
		return notes;
	}

}
