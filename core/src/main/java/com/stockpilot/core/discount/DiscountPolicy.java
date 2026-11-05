package com.stockpilot.core.discount;

import java.math.BigDecimal;

public interface DiscountPolicy {

    BigDecimal calculate(BigDecimal subtotal);
}
