package com.stockpilot.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

class JwtPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(Config.class);

    @Test
    void shouldFailStartupWhenSecretIsMissing() {
        runner.withPropertyValues("security.jwt.secret=").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void shouldFailStartupWhenSecretIsTooShort() {
        runner.withPropertyValues("security.jwt.secret=too-short").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void shouldBindSecretAndDefaultExpiration() {
        runner.withPropertyValues("security.jwt.secret=0123456789-0123456789-0123456789-ab").run(context -> {
            JwtProperties properties = context.getBean(JwtProperties.class);
            assertThat(properties.expiration()).isEqualTo(Duration.ofHours(1));
        });
    }

    @Test
    void shouldBindCustomExpiration() {
        runner.withPropertyValues("security.jwt.secret=0123456789-0123456789-0123456789-ab",
                "security.jwt.expiration=15m").run(context ->
                assertThat(context.getBean(JwtProperties.class).expiration()).isEqualTo(Duration.ofMinutes(15)));
    }
}
