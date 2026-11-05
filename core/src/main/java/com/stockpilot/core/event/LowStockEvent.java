package com.stockpilot.core.event;

public record LowStockEvent(Long productId, String sku, String productName, int quantity, int minimumQuantity) {
}
