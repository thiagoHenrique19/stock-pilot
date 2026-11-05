package com.stockpilot.core.port.in.dto;

import com.stockpilot.core.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        PaymentMethod paymentMethod,
        List<SaleItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        Instant createdAt) {
}
