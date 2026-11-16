package br.com.zelo.platform.beneficiary;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "beneficiary_documents")
public class BeneficiaryDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "beneficiary_id", nullable = false)
	private Long beneficiaryId;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "content_type", nullable = false, length = 50)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "storage_key", nullable = false, unique = true, length = 64)
	private String storageKey;

	@Column(name = "uploaded_at", nullable = false, updatable = false)
	private Instant uploadedAt;

	@Column(name = "uploaded_by", nullable = false)
	private Long uploadedBy;

	protected BeneficiaryDocument() {
	}

	public BeneficiaryDocument(Long beneficiaryId, String title, String fileName, String contentType, long sizeBytes,
			String storageKey, Long uploadedBy) {
		this.beneficiaryId = beneficiaryId;
		this.title = title;
		this.fileName = fileName;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.storageKey = storageKey;
		this.uploadedBy = uploadedBy;
	}

	@PrePersist
	void onCreate() {
		this.uploadedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getBeneficiaryId() {
		return beneficiaryId;
	}

	public String getTitle() {
		return title;
	}

	public String getFileName() {
		return fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}

	public Long getUploadedBy() {
		return uploadedBy;
	}

}
