package com.stockpilot.core.port.in.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        int quantity,
        int minimumQuantity,
        boolean lowStock,
        boolean active) {
}
