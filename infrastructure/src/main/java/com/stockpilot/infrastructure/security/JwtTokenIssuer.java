package com.stockpilot.infrastructure.security;

import com.stockpilot.core.domain.User;
import com.stockpilot.core.port.out.AccessToken;
import com.stockpilot.core.port.out.TokenIssuer;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class JwtTokenIssuer implements TokenIssuer {

    static final String ISSUER = "https://stock-pilot.local";
    static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new AccessToken(token, properties.expiration());
    }
}
