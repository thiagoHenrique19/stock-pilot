package com.stockpilot.core.port.in.dto;

import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.SaleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        String receiptNumber,
        SaleStatus status,
        PaymentMethod paymentMethod,
        List<SaleItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        BigDecimal amountReceived,
        BigDecimal change,
        Instant createdAt,
        Instant cancelledAt) {
}
