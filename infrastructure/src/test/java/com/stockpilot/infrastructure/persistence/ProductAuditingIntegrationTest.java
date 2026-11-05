package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import com.stockpilot.core.port.in.dto.ProductRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

class ProductAuditingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ManageProductsUseCase products;
    @Autowired private JdbcTemplate jdbc;

    private ProductRequest request() {
        return new ProductRequest("AUD-" + UUID.randomUUID().toString().substring(0, 8), "Audited", null,
                new BigDecimal("5.00"), 1, 0, null);
    }

    private Map<String, Object> row(long id) {
        return jdbc.queryForMap("SELECT created_at, updated_at, created_by, updated_by FROM product WHERE id = ?", id);
    }

    @Test
    @WithMockUser(username = "alice@shop.com", roles = "ADMIN")
    void shouldRecordWhoAndWhenOnCreate() {
        long id = products.register(request()).id();

        Map<String, Object> row = row(id);

        assertThat(row.get("created_by")).isEqualTo("alice@shop.com");
        assertThat(row.get("updated_by")).isEqualTo("alice@shop.com");
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isEqualTo(row.get("created_at"));
    }

    @Test
    void shouldFallBackToSystemWhenNobodyIsLoggedIn() {
        long id = products.register(request()).id();

        assertThat(row(id).get("created_by")).isEqualTo("system");
    }
}
