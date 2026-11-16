package br.com.zelo.platform.assistance;

import java.time.LocalDate;

public record DeliveryResponse(Long id, Long itemId, String itemName, Long beneficiaryId, String beneficiaryName,
		int quantity, LocalDate deliveredAt, String notes, String recordedBy) {
}
