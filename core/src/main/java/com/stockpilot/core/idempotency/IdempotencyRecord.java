package com.stockpilot.core.idempotency;

import java.time.Instant;

public record IdempotencyRecord(String scope, String key, String requestHash, Status status,
                                String responseSnapshot, Instant createdAt) {

    public enum Status { IN_PROGRESS, COMPLETED }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }
}
