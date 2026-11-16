package br.com.zelo.platform.shared.exception;

import java.util.Map;

public class ConflictException extends RuntimeException {

	private final Map<String, Object> properties;

	public ConflictException(String message) {
		this(message, Map.of());
	}

	// Properties go into the ProblemDetail, so the client can act on the conflict (for example, reactivate).
	public ConflictException(String message, Map<String, Object> properties) {
		super(message);
		this.properties = Map.copyOf(properties);
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

}
