package com.stockpilot.core.port.in.dto;

public record LowStockItem(Long productId, String sku, String name, int quantity, int minimumQuantity, int shortfall) {
}
