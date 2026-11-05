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

    @Test
    void shouldNormalizeSkuToUpperCase() {
        Product product = Product.create(" abc-1 ", "Tea", new BigDecimal("3.00"), 1, 0);

        assertThat(product.getSku()).isEqualTo("ABC-1");
    }

    @Test
    void shouldStartActiveWithCategoryAndDescription() {
        Category category = Category.create("Drinks", null);

        Product product = Product.create("T-1", "Tea", "Green tea", new BigDecimal("3.00"), 1, 0, category);

        assertThat(product.isActive()).isTrue();
        assertThat(product.getCategory()).isSameAs(category);
        assertThat(product.getDescription()).isEqualTo("Green tea");
    }

    @Test
    void shouldDeactivateAndReactivate() {
        Product product = product(5, 1);

        product.deactivate();
        assertThat(product.isActive()).isFalse();
        product.reactivate();
        assertThat(product.isActive()).isTrue();
    }

    @Test
    void shouldUpdateDetailsKeepingSkuAndStock() {
        Product product = product(5, 1);
        Category category = Category.create("Drinks", null);

        product.update("Coffee 1kg", "Strong", new BigDecimal("35.00"), 4, category);

        assertThat(product.getName()).isEqualTo("Coffee 1kg");
        assertThat(product.getDescription()).isEqualTo("Strong");
        assertThat(product.getPrice()).isEqualByComparingTo("35.00");
        assertThat(product.getMinimumQuantity()).isEqualTo(4);
        assertThat(product.getCategory()).isSameAs(category);
        assertThat(product.getSku()).isEqualTo("SKU-1");
        assertThat(product.getQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRejectInvalidUpdate() {
        Product product = product(5, 1);

        assertThatThrownBy(() -> product.update("X", null, new BigDecimal("-1"), 1, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> product.update("X", null, BigDecimal.ONE, -1, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldAdjustStockUpAndDown() {
        Product product = product(10, 2);

        product.adjustStock(5);
        assertThat(product.getQuantity()).isEqualTo(15);
        product.adjustStock(-15);
        assertThat(product.getQuantity()).isZero();
    }

    @Test
    void shouldNeverAdjustBelowZero() {
        Product product = product(3, 1);

        assertThatThrownBy(() -> product.adjustStock(-4))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("below zero");
        assertThat(product.getQuantity()).isEqualTo(3);
    }

    @Test
    void shouldRejectZeroAdjustment() {
        assertThatThrownBy(() -> product(3, 1).adjustStock(0)).isInstanceOf(BusinessRuleException.class);
    }
}
