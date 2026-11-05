package com.stockpilot.core.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DiscountPolicyTest {

    @Test
    void noDiscountShouldReturnZero() {
        assertThat(new NoDiscountPolicy().calculate(new BigDecimal("100"))).isEqualByComparingTo("0");
    }

    @Test
    void percentageDiscountShouldCalculateAmount() {
        DiscountPolicy policy = new PercentageDiscountPolicy(new BigDecimal("10"));

        assertThat(policy.calculate(new BigDecimal("200.00"))).isEqualByComparingTo("20.00");
    }

    @Test
    void factoryShouldReturnNoDiscountWhenPercentageIsNullOrZero() {
        assertThat(DiscountPolicyFactory.fromPercentage(null)).isInstanceOf(NoDiscountPolicy.class);
        assertThat(DiscountPolicyFactory.fromPercentage(BigDecimal.ZERO)).isInstanceOf(NoDiscountPolicy.class);
    }

    @Test
    void factoryShouldReturnPercentagePolicy() {
        assertThat(DiscountPolicyFactory.fromPercentage(new BigDecimal("5")))
                .isInstanceOf(PercentageDiscountPolicy.class);
    }

    @Test
    void shouldRejectPercentageOutOfRange() {
        assertThatThrownBy(() -> new PercentageDiscountPolicy(new BigDecimal("101")))
                .isInstanceOf(BusinessRuleException.class);
    }
}
