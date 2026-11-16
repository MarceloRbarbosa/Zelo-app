package br.com.zelo.platform.assistance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// active defaults to true when omitted; inactive items no longer take new receipts or deliveries.
public record ItemRequest(@NotBlank @Size(max = 100) String name, @Size(max = 255) String description,
		Boolean active) {
}
