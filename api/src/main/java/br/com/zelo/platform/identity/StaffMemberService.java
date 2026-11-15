package br.com.zelo.platform.identity;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;

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
		if (request.role() == StaffRole.ROLE_BENEFICIARY) {
			throw new InvalidRequestException("Role ROLE_BENEFICIARY is not available yet");
		}
		String email = request.email().toLowerCase(Locale.ROOT);
		if (repository.existsByEmail(email)) {
			throw new ConflictException("E-mail already registered");
		}
		StaffMember staff = new StaffMember(request.fullName(), email, passwordEncoder.encode(request.password()),
				request.memberType(), request.role());
		return StaffMemberResponse.from(repository.save(staff));
	}

}
