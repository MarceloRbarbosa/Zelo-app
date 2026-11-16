package br.com.zelo.platform.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The note records who asked and how (for example, "pedido da mãe por escrito em 16/11"), without personal data.
public record DataRemovalRequest(@NotBlank @Size(max = 255) String note) {
}
