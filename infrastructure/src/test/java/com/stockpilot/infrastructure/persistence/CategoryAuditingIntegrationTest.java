package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.port.in.ManageCategoriesUseCase;
import com.stockpilot.core.port.in.dto.CategoryRequest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "alice@shop.com", roles = "ADMIN")
class CategoryAuditingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ManageCategoriesUseCase categories;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void shouldRecordTheCreatorOfACategory() {
        long id = categories.create(new CategoryRequest("Cat-" + UUID.randomUUID().toString().substring(0, 8), "x")).id();

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_by, updated_by, created_at FROM category WHERE id = ?", id);

        assertThat(row.get("created_by")).isEqualTo("alice@shop.com");
        assertThat(row.get("updated_by")).isEqualTo("alice@shop.com");
        assertThat(row.get("created_at")).isNotNull();
    }
}
