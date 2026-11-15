package br.com.zelo.platform.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
		@NotBlank String currentPassword,
		// BCrypt only uses the first 72 bytes of a password.
		@NotBlank @Size(min = 8, max = 72) String newPassword) {
}
