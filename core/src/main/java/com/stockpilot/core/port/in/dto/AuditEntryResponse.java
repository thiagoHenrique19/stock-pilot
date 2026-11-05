package com.stockpilot.core.port.in.dto;

import java.time.Instant;

public record AuditEntryResponse(Long id, String field, String oldValue, String newValue, String changedBy,
                                 Instant changedAt) {
}
