package br.com.zelo.platform.assistance;

import java.time.LocalDate;

public record ReceiptResponse(Long id, Long itemId, String itemName, int quantity, LocalDate receivedAt, String donor,
		String notes) {
}
