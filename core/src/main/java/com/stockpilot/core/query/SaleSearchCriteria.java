package com.stockpilot.core.query;

import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.SaleStatus;
import com.stockpilot.core.exception.InvalidRequestException;
import java.time.LocalDate;

/** Filters used by the sales listing. Both dates are inclusive and optional. */
public record SaleSearchCriteria(LocalDate from, LocalDate to, PaymentMethod paymentMethod, SaleStatus status) {

    public SaleSearchCriteria {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' date must not be after 'to' date");
        }
    }
}
