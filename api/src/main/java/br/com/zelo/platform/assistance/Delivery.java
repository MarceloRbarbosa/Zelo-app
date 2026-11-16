package br.com.zelo.platform.assistance;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assistance_deliveries")
public class Delivery {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "beneficiary_id", nullable = false)
	private Long beneficiaryId;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "delivered_at", nullable = false)
	private LocalDate deliveredAt;

	@Column(name = "notes")
	private String notes;

	@Column(name = "recorded_by", nullable = false)
	private Long recordedBy;

	protected Delivery() {
	}

	public Delivery(Long itemId, Long beneficiaryId, int quantity, LocalDate deliveredAt, String notes,
			Long recordedBy) {
		this.itemId = itemId;
		this.beneficiaryId = beneficiaryId;
		this.quantity = quantity;
		this.deliveredAt = deliveredAt;
		this.notes = notes;
		this.recordedBy = recordedBy;
	}

	public Long getId() {
		return id;
	}

	public Long getItemId() {
		return itemId;
	}

	public Long getBeneficiaryId() {
		return beneficiaryId;
	}

	public int getQuantity() {
		return quantity;
	}

	public LocalDate getDeliveredAt() {
		return deliveredAt;
	}

	public String getNotes() {
		return notes;
	}

	public Long getRecordedBy() {
		return recordedBy;
	}

}
