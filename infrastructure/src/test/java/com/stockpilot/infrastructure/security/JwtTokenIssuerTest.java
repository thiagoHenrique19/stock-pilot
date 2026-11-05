package com.stockpilot.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.domain.Role;
import com.stockpilot.core.domain.User;
import com.stockpilot.core.port.out.AccessToken;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtTokenIssuerTest {

    private static final String SECRET = "0123456789-0123456789-0123456789-ab";

    private final JwtConfig config = new JwtConfig();

    private JwtTokenIssuer issuer(String secret, Duration expiration, Clock clock) {
        JwtProperties properties = new JwtProperties(secret, expiration);
        JwtEncoder encoder = config.jwtEncoder(config.jwtSecretKey(properties));
        return new JwtTokenIssuer(encoder, properties, clock);
    }

    private JwtDecoder decoder(String secret) {
        return config.jwtDecoder(config.jwtSecretKey(new JwtProperties(secret, Duration.ofHours(1))));
    }

    @Test
    void shouldIssueTokenWithSubjectRoleAndExpiration() {
        User user = User.create("Admin@Shop.com", "hash", Role.ADMIN);

        AccessToken token = issuer(SECRET, Duration.ofMinutes(15), Clock.systemUTC()).issue(user);
        Jwt jwt = decoder(SECRET).decode(token.value());

        assertThat(token.validity()).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getSubject()).isEqualTo("admin@shop.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getIssuer().toString()).isEqualTo("https://stock-pilot.local");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        User user = User.create("a@b.com", "hash", Role.CASHIER);
        AccessToken token = issuer("another-secret-another-secret-123456", Duration.ofMinutes(5), Clock.systemUTC()).issue(user);

        assertThatThrownBy(() -> decoder(SECRET).decode(token.value())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectExpiredToken() {
        User user = User.create("a@b.com", "hash", Role.CASHIER);
        AccessToken token = issuer(SECRET, Duration.ofMinutes(5), Clock.offset(Clock.systemUTC(), Duration.ofHours(-2))).issue(user);

        assertThatThrownBy(() -> decoder(SECRET).decode(token.value())).isInstanceOf(JwtValidationException.class);
    }
}
