package com.stockpilot.core.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(@NotNull Integer quantity, @NotBlank String reason) {
}
