package com.stockpilot.core.port.in.dto;

import com.stockpilot.core.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record SaleRequest(
        @NotNull PaymentMethod paymentMethod,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        BigDecimal amountReceived,
        @NotEmpty List<@Valid SaleItemRequest> items) {
}
