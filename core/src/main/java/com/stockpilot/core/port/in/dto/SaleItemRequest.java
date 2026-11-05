package com.stockpilot.core.port.in.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(@NotNull Long productId, @Min(1) int quantity) {
}
