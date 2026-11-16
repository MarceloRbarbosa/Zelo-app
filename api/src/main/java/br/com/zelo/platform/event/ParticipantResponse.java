package br.com.zelo.platform.event;

import java.time.Instant;

public record ParticipantResponse(
		Long beneficiaryId,
		String fullName,
		Instant registeredAt,
		boolean attended) {
}
