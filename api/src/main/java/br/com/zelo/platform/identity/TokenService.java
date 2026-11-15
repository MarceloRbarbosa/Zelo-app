package br.com.zelo.platform.identity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.zelo.platform.shared.security.SecurityConfig;

@Service
public class TokenService {

	static final Duration TOKEN_VALIDITY = Duration.ofHours(8);

	private final JwtEncoder encoder;

	public TokenService(JwtEncoder encoder) {
		this.encoder = encoder;
	}

	public LoginResponse issue(StaffMember staff, Instant now) {
		Instant expiresAt = now.plus(TOKEN_VALIDITY);
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(SecurityConfig.ISSUER)
			.subject(String.valueOf(staff.getId()))
			.issuedAt(now)
			.expiresAt(expiresAt)
			.claim("email", staff.getEmail())
			.claim(SecurityConfig.ROLES_CLAIM, List.of(staff.getRole().name()))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new LoginResponse(token, expiresAt, staff.isMustChangePassword());
	}

}
