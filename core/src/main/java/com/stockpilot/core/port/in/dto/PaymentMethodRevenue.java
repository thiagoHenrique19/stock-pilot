package com.stockpilot.core.port.in.dto;

import com.stockpilot.core.domain.PaymentMethod;
import java.math.BigDecimal;

public record PaymentMethodRevenue(PaymentMethod paymentMethod, long salesCount, BigDecimal revenue) {
}
