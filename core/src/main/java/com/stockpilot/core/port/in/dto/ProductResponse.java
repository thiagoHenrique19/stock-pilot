package com.stockpilot.core.port.in.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id, String sku, String name, BigDecimal price, int quantity, int minimumQuantity, boolean lowStock) {
}
