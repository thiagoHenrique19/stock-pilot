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

    private long createAs(String user) {
        var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(user, null, org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        return products.register(request()).id();
    }

    private void actAs(String user) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(user, null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @org.junit.jupiter.api.AfterEach
    void clearContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void shouldStampTheUpdaterWithoutTouchingTheCreator() throws Exception {
        long id = createAs("alice@shop.com");
        Thread.sleep(20);

        actAs("bob@shop.com");
        products.update(id, new com.stockpilot.core.port.in.dto.ProductUpdateRequest("Renamed", null,
                new BigDecimal("6.00"), 0, null));

        Map<String, Object> row = row(id);
        assertThat(row.get("created_by")).isEqualTo("alice@shop.com");
        assertThat(row.get("updated_by")).isEqualTo("bob@shop.com");
        assertThat(((java.sql.Timestamp) row.get("updated_at")).after((java.sql.Timestamp) row.get("created_at"))).isTrue();
    }

    @Test
    void shouldAlsoStampStockChangesAndKeepTheCreatedAtUntouched() {
        long id = createAs("alice@shop.com");
        Object createdAt = row(id).get("created_at");

        actAs("carol@shop.com");
        products.restock(id, new com.stockpilot.core.port.in.dto.RestockRequest(3));

        assertThat(row(id).get("updated_by")).isEqualTo("carol@shop.com");
        assertThat(row(id).get("created_at")).isEqualTo(createdAt);
    }

    @Test
    void shouldLeaveTheRowAloneOnReads() {
        long id = createAs("alice@shop.com");
        Object updatedAt = row(id).get("updated_at");

        actAs("dave@shop.com");
        products.findById(id);

        assertThat(row(id).get("updated_at")).isEqualTo(updatedAt);
        assertThat(row(id).get("updated_by")).isEqualTo("alice@shop.com");
    }
}
