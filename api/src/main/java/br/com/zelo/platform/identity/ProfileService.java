package br.com.zelo.platform.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@Service
public class ProfileService {

	private final StaffMemberRepository repository;

	private final PasswordEncoder passwordEncoder;

	public ProfileService(StaffMemberRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public ProfileResponse get(Long staffId) {
		return ProfileResponse.from(find(staffId));
	}

	@Transactional
	public ProfileResponse update(Long staffId, ProfileUpdateRequest request) {
		StaffMember staff = find(staffId);
		staff.rename(request.fullName());
		return ProfileResponse.from(staff);
	}

	@Transactional
	public void changePassword(Long staffId, PasswordChangeRequest request) {
		StaffMember staff = find(staffId);
		if (!passwordEncoder.matches(request.currentPassword(), staff.getPasswordHash())) {
			throw new InvalidRequestException("Current password is incorrect");
		}
		if (request.newPassword().equals(request.currentPassword())) {
			throw new InvalidRequestException("New password must be different from the current one");
		}
		staff.changePassword(passwordEncoder.encode(request.newPassword()));
	}

	private StaffMember find(Long staffId) {
		return repository.findById(staffId)
			.orElseThrow(() -> new NotFoundException("Staff member " + staffId + " not found"));
	}

}
