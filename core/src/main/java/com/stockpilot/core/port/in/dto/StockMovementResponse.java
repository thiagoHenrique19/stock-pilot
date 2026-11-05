package com.stockpilot.core.port.in.dto;

import com.stockpilot.core.domain.MovementType;
import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long productId,
        MovementType type,
        int quantity,
        int balanceAfter,
        String reason,
        String reference,
        Instant createdAt) {
}
