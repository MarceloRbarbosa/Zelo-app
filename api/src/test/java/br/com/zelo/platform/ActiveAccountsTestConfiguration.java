package br.com.zelo.platform;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import br.com.zelo.platform.shared.security.AccountState;
import br.com.zelo.platform.shared.security.AccountStatusChecker;

// For feature tests that authenticate with mock JWTs (SecurityTestSupport): their subjects are not real
// staff members, so the per-request account check is replaced by one that treats every account as active.
@TestConfiguration(proxyBeanMethods = false)
public class ActiveAccountsTestConfiguration {

	@Bean
	@Primary
	AccountStatusChecker activeAccountStatusChecker() {
		return subject -> AccountState.ACTIVE;
	}

}
