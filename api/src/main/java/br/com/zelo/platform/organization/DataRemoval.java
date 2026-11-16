package br.com.zelo.platform.organization;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "data_removals")
public class DataRemoval {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "beneficiary_id", nullable = false)
	private Long beneficiaryId;

	@Column(name = "removed_at", nullable = false, updatable = false)
	private Instant removedAt;

	@Column(name = "removed_by", nullable = false)
	private Long removedBy;

	@Column(name = "note")
	private String note;

	protected DataRemoval() {
	}

	public DataRemoval(Long beneficiaryId, Long removedBy, String note) {
		this.beneficiaryId = beneficiaryId;
		this.removedBy = removedBy;
		this.note = note;
	}

	@PrePersist
	void onCreate() {
		this.removedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getBeneficiaryId() {
		return beneficiaryId;
	}

	public Instant getRemovedAt() {
		return removedAt;
	}

	public Long getRemovedBy() {
		return removedBy;
	}

	public String getNote() {
		return note;
	}

}
