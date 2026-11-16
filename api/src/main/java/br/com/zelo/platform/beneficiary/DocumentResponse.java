package br.com.zelo.platform.beneficiary;

import java.time.Instant;

public record DocumentResponse(Long id, String title, String fileName, String contentType, long sizeBytes,
		Instant uploadedAt) {

	static DocumentResponse from(BeneficiaryDocument document) {
		return new DocumentResponse(document.getId(), document.getTitle(), document.getFileName(),
				document.getContentType(), document.getSizeBytes(), document.getUploadedAt());
	}

}
