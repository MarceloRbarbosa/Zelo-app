package br.com.zelo.platform.identity;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.security.AccountState;
import br.com.zelo.platform.shared.security.AccountStatusChecker;

@Component
public class StaffAccountStatusChecker implements AccountStatusChecker {

	private final StaffMemberRepository repository;

	public StaffAccountStatusChecker(StaffMemberRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public AccountState stateOf(String subject) {
		Long id = parseId(subject);
		StaffMember staff = id == null ? null : repository.findById(id).orElse(null);
		if (staff == null || !staff.isActive()) {
			return AccountState.DISABLED;
		}
		return staff.isMustChangePassword() ? AccountState.PASSWORD_CHANGE_REQUIRED : AccountState.ACTIVE;
	}

	private static Long parseId(String subject) {
		try {
			return Long.valueOf(subject);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

}
