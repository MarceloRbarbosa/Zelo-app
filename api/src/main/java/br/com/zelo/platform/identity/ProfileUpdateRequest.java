package br.com.zelo.platform.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(@NotBlank @Size(max = 150) String fullName) {
}
