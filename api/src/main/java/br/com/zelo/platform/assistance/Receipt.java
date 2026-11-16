package br.com.zelo.platform.assistance;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assistance_receipts")
public class Receipt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "received_at", nullable = false)
	private LocalDate receivedAt;

	@Column(name = "donor", length = 150)
	private String donor;

	@Column(name = "notes")
	private String notes;

	@Column(name = "recorded_by", nullable = false)
	private Long recordedBy;

	protected Receipt() {
	}

	public Receipt(Long itemId, int quantity, LocalDate receivedAt, String donor, String notes, Long recordedBy) {
		this.itemId = itemId;
		this.quantity = quantity;
		this.receivedAt = receivedAt;
		this.donor = donor;
		this.notes = notes;
		this.recordedBy = recordedBy;
	}

	public Long getId() {
		return id;
	}

	public Long getItemId() {
		return itemId;
	}

	public int getQuantity() {
		return quantity;
	}

	public LocalDate getReceivedAt() {
		return receivedAt;
	}

	public String getDonor() {
		return donor;
	}

	public String getNotes() {
		return notes;
	}

	public Long getRecordedBy() {
		return recordedBy;
	}

}
