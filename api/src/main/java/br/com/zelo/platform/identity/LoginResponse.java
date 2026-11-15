package br.com.zelo.platform.identity;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt) {
}
