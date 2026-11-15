package br.com.zelo.platform.identity;

public record ProfileResponse(Long id, String fullName, String email, StaffRole role, boolean mustChangePassword) {

	static ProfileResponse from(StaffMember staff) {
		return new ProfileResponse(staff.getId(), staff.getFullName(), staff.getEmail(), staff.getRole(),
				staff.isMustChangePassword());
	}

}
