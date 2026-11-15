package br.com.zelo.platform.identity;

public record StaffMemberResponse(
		Long id,
		String fullName,
		String email,
		StaffMemberType memberType,
		StaffRole role,
		StaffStatus status,
		boolean mustChangePassword) {

	static StaffMemberResponse from(StaffMember staff) {
		return new StaffMemberResponse(staff.getId(), staff.getFullName(), staff.getEmail(), staff.getMemberType(),
				staff.getRole(), staff.getStatus(), staff.isMustChangePassword());
	}

}
