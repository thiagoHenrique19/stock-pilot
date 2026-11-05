package com.stockpilot.core.discount;

import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;

public final class DiscountPolicyFactory {

    private DiscountPolicyFactory() {
    }

    public static DiscountPolicy fromPercentage(BigDecimal percentage) {
        if (isAbsent(percentage)) {
            return new NoDiscountPolicy();
        }
        return new PercentageDiscountPolicy(percentage);
    }

    public static DiscountPolicy create(BigDecimal percentage, BigDecimal amount) {
        if (!isAbsent(percentage) && !isAbsent(amount)) {
            throw new BusinessRuleException("Provide either a discount percentage or a discount amount, not both");
        }
        if (!isAbsent(amount)) {
            return new FixedAmountDiscountPolicy(amount);
        }
        return fromPercentage(percentage);
    }

    private static boolean isAbsent(BigDecimal value) {
        return value == null || value.signum() == 0;
    }
}
