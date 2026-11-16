package br.com.zelo.platform.assistance;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DeliveryRequest(
		@NotNull Long itemId,
		@NotNull Long beneficiaryId,
		@NotNull @Positive Integer quantity,
		@NotNull @PastOrPresent LocalDate deliveredAt,
		@Size(max = 255) String notes) {
}
