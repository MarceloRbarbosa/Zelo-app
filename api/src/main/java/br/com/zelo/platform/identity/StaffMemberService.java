package br.com.zelo.platform.identity;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.ForbiddenException;
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

	@Transactional(readOnly = true)
	public List<StaffSummary> findSummaries(Collection<Long> ids) {
		return repository.findAllById(ids)
			.stream()
			.map(staff -> new StaffSummary(staff.getId(), staff.getFullName()))
			.toList();
	}

	// Used when picking who is responsible for an activity or event.
	@Transactional(readOnly = true)
	public Set<Long> requireExisting(Collection<Long> ids) {
		Set<Long> missing = new TreeSet<>(ids);
		repository.findAllById(ids).forEach(staff -> missing.remove(staff.getId()));
		if (!missing.isEmpty()) {
			throw new InvalidRequestException("Staff members not found: " + missing);
		}
		return Set.copyOf(ids);
	}

	@Transactional
	public StaffMemberResponse create(StaffMemberRequest request, StaffRole requesterRole) {
		requireManageable(requesterRole, request.role());
		String email = request.email().toLowerCase(Locale.ROOT);
		if (repository.existsByEmail(email)) {
			throw new ConflictException("E-mail already registered");
		}
		StaffMember staff = StaffMember.withProvisionalPassword(request.fullName(), email,
				passwordEncoder.encode(request.password()), request.memberType(), request.role());
		return StaffMemberResponse.from(repository.save(staff));
	}

	@Transactional
	public StaffMemberResponse block(Long id, Long currentStaffId, StaffRole requesterRole) {
		if (id.equals(currentStaffId)) {
			throw new InvalidRequestException("You cannot block your own account");
		}
		StaffMember staff = find(id);
		requireManageable(requesterRole, staff.getRole());
		staff.block();
		return StaffMemberResponse.from(staff);
	}

	@Transactional
	public StaffMemberResponse unblock(Long id, StaffRole requesterRole) {
		StaffMember staff = find(id);
		requireManageable(requesterRole, staff.getRole());
		staff.unblock();
		return StaffMemberResponse.from(staff);
	}

	private static void requireManageable(StaffRole requesterRole, StaffRole targetRole) {
		if (!requesterRole.canManage(targetRole)) {
			throw new ForbiddenException("Your profile cannot manage members with role " + targetRole);
		}
	}

	private StaffMember find(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Staff member " + id + " not found"));
	}

}
