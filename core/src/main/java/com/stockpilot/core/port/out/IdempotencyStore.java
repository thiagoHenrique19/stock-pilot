package com.stockpilot.core.port.out;

import com.stockpilot.core.idempotency.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyStore {

    /** Claims the key. Returns false when someone else already holds it; the unique key decides races. */
    boolean reserve(String scope, String key, String requestHash);

    Optional<IdempotencyRecord> find(String scope, String key);

    /** Stores the response and marks the key completed. Meant to run inside the business transaction. */
    void complete(String scope, String key, String responseSnapshot);

    /** Frees a reservation that never completed so the client can retry the same key. Completed keys are kept. */
    void release(String scope, String key);
}
