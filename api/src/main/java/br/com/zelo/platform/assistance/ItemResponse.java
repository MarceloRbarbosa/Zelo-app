package br.com.zelo.platform.assistance;

// balance = everything received minus everything delivered.
public record ItemResponse(Long id, String name, String description, boolean active, long received, long delivered,
		long balance) {
}
