package com.stockpilot.infrastructure.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("security.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET must be configured")
        @Size(min = 32, message = "JWT secret must have at least 32 characters")
        String secret,
        @DefaultValue("1h") Duration expiration) {
}
