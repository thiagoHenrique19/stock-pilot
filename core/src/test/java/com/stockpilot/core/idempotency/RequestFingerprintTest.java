package com.stockpilot.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RequestFingerprintTest {

    @Test
    void shouldProduceTheKnownSha256ForAPayload() {
        assertThat(RequestFingerprint.of("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void shouldAlwaysBe64LowercaseHexCharacters() {
        assertThat(RequestFingerprint.of("")).matches("[0-9a-f]{64}");
        assertThat(RequestFingerprint.of("ação €")).matches("[0-9a-f]{64}");
    }

    @ParameterizedTest
    @CsvSource({
            "{\"q\":1}, {\"q\":1}, true",
            "{\"q\":1}, {\"q\":2}, false",
            "{\"q\":1}, {\"q\": 1}, false",
            "a, A, false"
    })
    void shouldMatchOnlyIdenticalPayloads(String left, String right, boolean same) {
        assertThat(RequestFingerprint.of(left).equals(RequestFingerprint.of(right))).isEqualTo(same);
    }
}
