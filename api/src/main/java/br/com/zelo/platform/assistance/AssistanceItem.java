package br.com.zelo.platform.assistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assistance_items")
public class AssistanceItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, unique = true, length = 100)
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	protected AssistanceItem() {
	}

	public AssistanceItem(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public void update(String name, String description, boolean active) {
		this.name = name;
		this.description = description;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isActive() {
		return active;
	}

}
