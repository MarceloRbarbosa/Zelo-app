package br.com.zelo.platform.shared.security;

// Implemented by the identity module, so shared/security does not depend on it.
public interface AccountStatusChecker {

	AccountState stateOf(String subject);

}
