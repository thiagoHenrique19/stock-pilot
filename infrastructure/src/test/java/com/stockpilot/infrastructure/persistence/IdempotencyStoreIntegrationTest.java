package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.idempotency.IdempotencyRecord;
import com.stockpilot.core.port.out.IdempotencyStore;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IdempotencyStoreIntegrationTest extends AbstractIntegrationTest {

    private static final String HASH = "a".repeat(64);

    @Autowired private IdempotencyStore store;

    private String newKey() {
        return UUID.randomUUID().toString();
    }

    @Test
    void shouldReserveAKeyOnlyOnce() {
        String key = newKey();

        assertThat(store.reserve("sales", key, HASH)).isTrue();
        assertThat(store.reserve("sales", key, HASH)).isFalse();
    }

    @Test
    void shouldKeepTheSameKeyIndependentAcrossScopes() {
        String key = newKey();

        assertThat(store.reserve("sales", key, HASH)).isTrue();
        assertThat(store.reserve("restock:1", key, HASH)).isTrue();
    }

    @Test
    void shouldFindAReservationAsInProgress() {
        String key = newKey();
        store.reserve("sales", key, HASH);

        IdempotencyRecord record = store.find("sales", key).orElseThrow();

        assertThat(record.status()).isEqualTo(IdempotencyRecord.Status.IN_PROGRESS);
        assertThat(record.requestHash()).isEqualTo(HASH);
        assertThat(record.responseSnapshot()).isNull();
        assertThat(store.find("sales", newKey())).isEmpty();
    }

    @Test
    void shouldStoreTheResponseSnapshotWhenCompleted() {
        String key = newKey();
        store.reserve("sales", key, HASH);

        store.complete("sales", key, "{\"id\":1}");

        IdempotencyRecord record = store.find("sales", key).orElseThrow();
        assertThat(record.isCompleted()).isTrue();
        assertThat(record.responseSnapshot()).isEqualTo("{\"id\":1}");
        assertThat(store.reserve("sales", key, HASH)).isFalse();
    }
}
