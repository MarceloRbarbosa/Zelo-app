package br.com.zelo.platform.assistance;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReceiptRequest(
		@NotNull Long itemId,
		@NotNull @Positive Integer quantity,
		@NotNull @PastOrPresent LocalDate receivedAt,
		@Size(max = 150) String donor,
		@Size(max = 255) String notes) {
}
