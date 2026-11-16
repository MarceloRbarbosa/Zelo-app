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
@Table(name = "enrollments")
public class Enrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "activity_id", nullable = false)
	private Long activityId;

	@Column(name = "beneficiary_id", nullable = false)
	private Long beneficiaryId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

	@Column(name = "left_at")
	private LocalDate leftAt;

	protected Enrollment() {
	}

	public Enrollment(Long activityId, Long beneficiaryId) {
		this.activityId = activityId;
		this.beneficiaryId = beneficiaryId;
	}

	// Leaving keeps the row, so the attendance history stays linked to the activity.
	public void leave(LocalDate leftAt) {
		this.status = EnrollmentStatus.LEFT;
		this.leftAt = leftAt;
	}

	public void rejoin() {
		this.status = EnrollmentStatus.ACTIVE;
		this.leftAt = null;
	}

	public boolean isActive() {
		return status == EnrollmentStatus.ACTIVE;
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

	public EnrollmentStatus getStatus() {
		return status;
	}

	public LocalDate getLeftAt() {
		return leftAt;
	}

}
