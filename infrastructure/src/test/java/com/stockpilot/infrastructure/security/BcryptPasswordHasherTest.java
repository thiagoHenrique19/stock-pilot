package com.stockpilot.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher();

    @Test
    void shouldHashWithBcryptAndVerify() {
        String hash = hasher.hash("s3cret-pass");

        assertThat(hash).startsWith("$2").isNotEqualTo("s3cret-pass");
        assertThat(hasher.matches("s3cret-pass", hash)).isTrue();
        assertThat(hasher.matches("other", hash)).isFalse();
    }

    @Test
    void shouldProduceDifferentHashesForTheSamePassword() {
        assertThat(hasher.hash("same")).isNotEqualTo(hasher.hash("same"));
    }
}
