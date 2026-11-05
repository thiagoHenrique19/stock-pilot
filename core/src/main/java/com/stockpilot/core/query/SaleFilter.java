package com.stockpilot.core.query;

import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.SaleStatus;
import java.time.Instant;

/** Persistence-level filter: {@code from} is inclusive and {@code to} is exclusive. */
public record SaleFilter(Instant from, Instant to, PaymentMethod paymentMethod, SaleStatus status) {
}
