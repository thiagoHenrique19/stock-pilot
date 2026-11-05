package com.stockpilot.core.discount;

import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscountPolicy implements DiscountPolicy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal percentage;

    public PercentageDiscountPolicy(BigDecimal percentage) {
        if (percentage.signum() < 0 || percentage.compareTo(HUNDRED) > 0) {
            throw new BusinessRuleException("Discount percentage must be between 0 and 100");
        }
        this.percentage = percentage;
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return subtotal.multiply(percentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
