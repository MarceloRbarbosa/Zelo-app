package br.com.zelo.platform.event;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_participants")
public class EventParticipant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false)
	private Long eventId;

	@Column(name = "beneficiary_id", nullable = false)
	private Long beneficiaryId;

	@Column(name = "registered_at", nullable = false, updatable = false)
	private Instant registeredAt;

	@Column(name = "attended", nullable = false)
	private boolean attended;

	protected EventParticipant() {
	}

	public EventParticipant(Long eventId, Long beneficiaryId) {
		this.eventId = eventId;
		this.beneficiaryId = beneficiaryId;
	}

	@PrePersist
	void onCreate() {
		this.registeredAt = Instant.now();
	}

	public void markAttendance(boolean attended) {
		this.attended = attended;
	}

	public Long getId() {
		return id;
	}

	public Long getEventId() {
		return eventId;
	}

	public Long getBeneficiaryId() {
		return beneficiaryId;
	}

	public Instant getRegisteredAt() {
		return registeredAt;
	}

	public boolean isAttended() {
		return attended;
	}

}
