package com.stockpilot.core.discount;

import java.math.BigDecimal;

public final class DiscountPolicyFactory {

    private DiscountPolicyFactory() {
    }

    public static DiscountPolicy fromPercentage(BigDecimal percentage) {
        if (percentage == null || percentage.signum() == 0) {
            return new NoDiscountPolicy();
        }
        return new PercentageDiscountPolicy(percentage);
    }
}
