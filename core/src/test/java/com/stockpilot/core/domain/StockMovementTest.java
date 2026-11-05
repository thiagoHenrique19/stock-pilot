package com.stockpilot.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StockMovementTest {

    private final Product product = Product.create("P-1", "Tea", new BigDecimal("3.00"), 10, 2);

    @Test
    void shouldCaptureProductBalanceAfterMovement() {
        product.removeStock(4);

        StockMovement movement = StockMovement.record(product, MovementType.OUT, -4, "Sale", "SALE-1");

        assertThat(movement.getProduct()).isSameAs(product);
        assertThat(movement.getType()).isEqualTo(MovementType.OUT);
        assertThat(movement.getQuantity()).isEqualTo(-4);
        assertThat(movement.getBalanceAfter()).isEqualTo(6);
        assertThat(movement.getReason()).isEqualTo("Sale");
        assertThat(movement.getReference()).isEqualTo("SALE-1");
        assertThat(movement.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRejectZeroQuantity() {
        assertThatThrownBy(() -> StockMovement.record(product, MovementType.IN, 0, "x", null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRequireReasonForAdjustments() {
        assertThatThrownBy(() -> StockMovement.record(product, MovementType.ADJUSTMENT, 1, " ", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void shouldRequireSignConsistentWithType() {
        assertThatThrownBy(() -> StockMovement.record(product, MovementType.IN, -1, "x", null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> StockMovement.record(product, MovementType.OUT, 1, "x", null))
                .isInstanceOf(BusinessRuleException.class);
    }
}
