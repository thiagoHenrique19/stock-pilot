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

    @Test
    void fixedAmountDiscountShouldReturnConfiguredAmount() {
        DiscountPolicy policy = new FixedAmountDiscountPolicy(new BigDecimal("15"));

        assertThat(policy.calculate(new BigDecimal("100.00"))).isEqualByComparingTo("15.00");
    }

    @Test
    void fixedAmountDiscountMayEqualSubtotal() {
        assertThat(new FixedAmountDiscountPolicy(new BigDecimal("50.00")).calculate(new BigDecimal("50.00")))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void fixedAmountDiscountCannotExceedSubtotal() {
        DiscountPolicy policy = new FixedAmountDiscountPolicy(new BigDecimal("50.01"));

        assertThatThrownBy(() -> policy.calculate(new BigDecimal("50.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceed");
    }

    @Test
    void fixedAmountDiscountMustNotBeNegative() {
        assertThatThrownBy(() -> new FixedAmountDiscountPolicy(new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void factoryShouldCreatePolicyFromPercentageOrAmount() {
        assertThat(DiscountPolicyFactory.create(new BigDecimal("10"), null)).isInstanceOf(PercentageDiscountPolicy.class);
        assertThat(DiscountPolicyFactory.create(null, new BigDecimal("5"))).isInstanceOf(FixedAmountDiscountPolicy.class);
        assertThat(DiscountPolicyFactory.create(null, null)).isInstanceOf(NoDiscountPolicy.class);
        assertThat(DiscountPolicyFactory.create(BigDecimal.ZERO, BigDecimal.ZERO)).isInstanceOf(NoDiscountPolicy.class);
    }

    @Test
    void factoryShouldRejectPercentageAndAmountTogether() {
        assertThatThrownBy(() -> DiscountPolicyFactory.create(new BigDecimal("10"), new BigDecimal("5")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not both");
    }
}
