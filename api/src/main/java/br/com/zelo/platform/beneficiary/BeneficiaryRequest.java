package br.com.zelo.platform.beneficiary;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record BeneficiaryRequest(
		@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Cpf String documentNumber,
		@NotNull @Past LocalDate birthDate,
		@NotBlank @Size(max = 20) String phoneNumber,
		@Size(max = 255) String address) {
}
