package com.stockpilot.core.port.in.dto;

import java.math.BigDecimal;

public record TopSellingProduct(int rank, Long productId, String sku, String name, long unitsSold, BigDecimal revenue) {
}
