package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "cashier@shop.com", roles = "CASHIER")
class SaleAuditingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private CheckoutSaleUseCase checkout;
    @Autowired private ManageProductsUseCase products;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void shouldRecordTheCashierOnTheSaleAndOnItsStockMovement() {
        long productId = products.register(new ProductRequest("SA-" + UUID.randomUUID().toString().substring(0, 8),
                "Audited sale", null, new BigDecimal("4.00"), 5, 0, null)).id();

        long saleId = checkout.checkout(new SaleRequest(PaymentMethod.PIX, null, null, null,
                List.of(new SaleItemRequest(productId, 2)))).id();

        assertThat(jdbc.queryForObject("SELECT created_by FROM sale WHERE id = ?", String.class, saleId))
                .isEqualTo("cashier@shop.com");
        assertThat(jdbc.queryForList("SELECT created_by FROM stock_movement WHERE product_id = ? AND quantity < 0",
                String.class, productId)).containsExactly("cashier@shop.com");
    }
}
