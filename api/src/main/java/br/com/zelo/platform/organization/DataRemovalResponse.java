package br.com.zelo.platform.organization;

import java.time.Instant;

public record DataRemovalResponse(Long id, Long beneficiaryId, Instant removedAt, String removedBy, String note) {
}
