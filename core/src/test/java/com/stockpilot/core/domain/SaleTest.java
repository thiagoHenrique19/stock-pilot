package com.stockpilot.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.discount.PercentageDiscountPolicy;
import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SaleTest {

    private final Product coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 50, 5);
    private final Product milk = Product.create("M-1", "Milk", new BigDecimal("5.50"), 50, 5);

    @Test
    void shouldSumItemsIntoSubtotal() {
        Sale sale = Sale.open(PaymentMethod.CASH);

        sale.addItem(coffee, 2);
        sale.addItem(milk, 4);

        assertThat(sale.getSubtotal()).isEqualByComparingTo("62.00");
        assertThat(sale.getTotal()).isEqualByComparingTo("62.00");
    }

    @Test
    void shouldApplyDiscountToTotal() {
        Sale sale = Sale.open(PaymentMethod.CREDIT_CARD);
        sale.addItem(coffee, 5);

        sale.applyDiscount(new PercentageDiscountPolicy(new BigDecimal("10")));

        assertThat(sale.getDiscountAmount()).isEqualByComparingTo("10.00");
        assertThat(sale.getTotal()).isEqualByComparingTo("90.00");
    }

    @Test
    void shouldRejectInactiveProducts() {
        Product inactive = Product.create("I-1", "Old", new BigDecimal("1.00"), 5, 0);
        inactive.deactivate();
        Sale sale = Sale.open(PaymentMethod.CASH);

        assertThatThrownBy(() -> sale.addItem(inactive, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");
    }
}
