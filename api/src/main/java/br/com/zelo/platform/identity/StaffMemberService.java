package br.com.zelo.platform.identity;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@Service
public class StaffMemberService {

	private final StaffMemberRepository repository;

	private final PasswordEncoder passwordEncoder;

	public StaffMemberService(StaffMemberRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<StaffMemberResponse> list() {
		return repository.findAllByOrderByFullNameAsc().stream().map(StaffMemberResponse::from).toList();
	}

	@Transactional
	public StaffMemberResponse create(StaffMemberRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		if (repository.existsByEmail(email)) {
			throw new ConflictException("E-mail already registered");
		}
		StaffMember staff = StaffMember.withProvisionalPassword(request.fullName(), email,
				passwordEncoder.encode(request.password()), request.memberType(), request.role());
		return StaffMemberResponse.from(repository.save(staff));
	}

	@Transactional
	public StaffMemberResponse block(Long id, Long currentStaffId) {
		if (id.equals(currentStaffId)) {
			throw new InvalidRequestException("You cannot block your own account");
		}
		StaffMember staff = find(id);
		staff.block();
		return StaffMemberResponse.from(staff);
	}

	@Transactional
	public StaffMemberResponse unblock(Long id) {
		StaffMember staff = find(id);
		staff.unblock();
		return StaffMemberResponse.from(staff);
	}

	private StaffMember find(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Staff member " + id + " not found"));
	}

}
