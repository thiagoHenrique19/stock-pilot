package com.stockpilot.core.port.out;

import com.stockpilot.core.idempotency.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyStore {

    /** Claims the key. Returns false when someone else already holds it; the unique key decides races. */
    boolean reserve(String scope, String key, String requestHash);

    Optional<IdempotencyRecord> find(String scope, String key);
}
