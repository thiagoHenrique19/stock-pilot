package com.stockpilot.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

    private Product product(int quantity, int minimum) {
        return Product.create("SKU-1", "Coffee 500g", new BigDecimal("19.90"), quantity, minimum);
    }

    @Test
    void shouldRemoveStock() {
        Product product = product(10, 2);

        product.removeStock(4);

        assertThat(product.getQuantity()).isEqualTo(6);
    }

    @Test
    void shouldRejectRemovalBeyondAvailableStock() {
        Product product = product(3, 1);

        assertThatThrownBy(() -> product.removeStock(4))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void shouldAddStock() {
        Product product = product(3, 1);

        product.addStock(7);

        assertThat(product.getQuantity()).isEqualTo(10);
    }

    @Test
    void shouldRejectNonPositiveMovements() {
        Product product = product(3, 1);

        assertThatThrownBy(() -> product.addStock(0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> product.removeStock(-1)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldBeLowStockWhenQuantityReachesMinimum() {
        Product product = product(5, 5);

        assertThat(product.isLowStock()).isTrue();
        product.addStock(1);
        assertThat(product.isLowStock()).isFalse();
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThatThrownBy(() -> Product.create("SKU-2", "Bad", new BigDecimal("-1"), 1, 1))
                .isInstanceOf(BusinessRuleException.class);
    }
}
