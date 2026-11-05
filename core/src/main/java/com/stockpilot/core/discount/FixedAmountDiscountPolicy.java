package com.stockpilot.core.discount;

import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FixedAmountDiscountPolicy implements DiscountPolicy {

    private final BigDecimal amount;

    public FixedAmountDiscountPolicy(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new BusinessRuleException("Discount amount cannot be negative");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        if (amount.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Discount amount cannot exceed the subtotal");
        }
        return amount;
    }
}
