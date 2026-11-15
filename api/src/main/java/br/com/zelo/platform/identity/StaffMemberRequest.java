package br.com.zelo.platform.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffMemberRequest(
		@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Email @Size(max = 100) String email,
		// BCrypt only uses the first 72 bytes of a password.
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotNull StaffMemberType memberType,
		@NotNull StaffRole role) {
}
