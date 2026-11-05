package com.stockpilot.core.port.in.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @Min(0) int quantity,
        @Min(0) int minimumQuantity,
        Long categoryId) {
}
