package com.stockpilot.core.discount;

import java.math.BigDecimal;

public class NoDiscountPolicy implements DiscountPolicy {

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }
}
