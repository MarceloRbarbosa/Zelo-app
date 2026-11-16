package br.com.zelo.platform.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class BeneficiaryDocumentServiceTest {

	@Test
	void detectsTypeFromTheFileContentNotItsName() {
		assertThat(BeneficiaryDocumentService.detectType("%PDF-1.7 ...".getBytes(StandardCharsets.US_ASCII)))
			.isEqualTo("application/pdf");
		assertThat(BeneficiaryDocumentService.detectType(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 }))
			.isEqualTo("image/jpeg");
		assertThat(BeneficiaryDocumentService
			.detectType(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00 }))
			.isEqualTo("image/png");
	}

	@Test
	void rejectsOtherContent() {
		assertThat(BeneficiaryDocumentService.detectType("<html>".getBytes(StandardCharsets.US_ASCII))).isNull();
		assertThat(BeneficiaryDocumentService.detectType(new byte[0])).isNull();
	}

}
