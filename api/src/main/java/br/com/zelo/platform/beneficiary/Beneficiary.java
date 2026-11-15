package br.com.zelo.platform.beneficiary;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(name = "document_number", unique = true, length = 11)
	private String documentNumber;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@Column(name = "phone_number", nullable = false, length = 20)
	private String phoneNumber;

	@Column(name = "address")
	private String address;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private BeneficiaryStatus status = BeneficiaryStatus.ACTIVE;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Beneficiary() {
	}

	public Beneficiary(String fullName, String documentNumber, LocalDate birthDate, String phoneNumber,
			String address) {
		this.fullName = fullName;
		this.documentNumber = documentNumber;
		this.birthDate = birthDate;
		this.phoneNumber = phoneNumber;
		this.address = address;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public void update(String fullName, String documentNumber, LocalDate birthDate, String phoneNumber,
			String address) {
		this.fullName = fullName;
		this.documentNumber = documentNumber;
		this.birthDate = birthDate;
		this.phoneNumber = phoneNumber;
		this.address = address;
	}

	public void deactivate() {
		this.status = BeneficiaryStatus.INACTIVE;
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getDocumentNumber() {
		return documentNumber;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public BeneficiaryStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
