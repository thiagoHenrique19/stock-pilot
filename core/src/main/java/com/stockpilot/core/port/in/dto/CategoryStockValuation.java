package com.stockpilot.core.port.in.dto;

import java.math.BigDecimal;

public record CategoryStockValuation(Long categoryId, String categoryName, long totalUnits, BigDecimal totalValue) {
}
