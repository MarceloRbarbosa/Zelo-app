package br.com.zelo.platform.assistance;

import java.time.LocalDate;

// recordedBy is the name of the member who registered it, taken from the logged-in account.
public record ReceiptResponse(Long id, Long itemId, String itemName, int quantity, LocalDate receivedAt, String donor,
		String notes, String recordedBy) {
}
