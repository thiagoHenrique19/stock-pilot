package com.stockpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Goes through the real login and JWT filter, so the user really travels from the token to the audit columns. */
class AuditTrailFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private String register(String adminToken, String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@shop.com";
        mockMvc.perform(post("/api/auth/register").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"long-pass-123\",\"role\":\"%s\"}".formatted(email, role)))
                .andExpect(status().isCreated());
        return email;
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    @Test
    void shouldAttributeEveryChangeToTheUserBehindTheToken() throws Exception {
        String root = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String managerEmail = register(root, "ADMIN");
        String manager = login(managerEmail, "long-pass-123");
        String cashierEmail = register(root, "CASHIER");
        String cashier = login(cashierEmail, "long-pass-123");

        int productId = JsonPath.read(mockMvc.perform(post("/api/products").header("Authorization", root)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"AT-%s\",\"name\":\"Trail\",\"price\":5.00,\"quantity\":10,\"minimumQuantity\":1}"
                        .formatted(UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(put("/api/products/" + productId).header("Authorization", manager)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Trail\",\"price\":7.50,\"minimumQuantity\":1}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/products/%d/deactivate".formatted(productId)).header("Authorization", root))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/products/%d/reactivate".formatted(productId)).header("Authorization", manager))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/sales").header("Authorization", cashier).contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentMethod\":\"PIX\",\"items\":[{\"productId\":%d,\"quantity\":1}]}".formatted(productId)))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT created_by FROM product WHERE id = ?", String.class, productId))
                .isEqualTo(ADMIN_EMAIL);
        assertThat(jdbc.queryForObject("SELECT updated_by FROM product WHERE id = ?", String.class, productId))
                .isEqualTo(cashierEmail);
        assertThat(jdbc.queryForObject("SELECT created_by FROM sale ORDER BY id DESC LIMIT 1", String.class))
                .isEqualTo(cashierEmail);

        mockMvc.perform(get("/api/products/%d/history".formatted(productId)).header("Authorization", root))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].field").value("active"))
                .andExpect(jsonPath("$.content[0].newValue").value("true"))
                .andExpect(jsonPath("$.content[0].changedBy").value(managerEmail))
                .andExpect(jsonPath("$.content[1].changedBy").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.content[2].field").value("price"))
                .andExpect(jsonPath("$.content[2].oldValue").value("5.00"))
                .andExpect(jsonPath("$.content[2].newValue").value("7.50"))
                .andExpect(jsonPath("$.content[2].changedBy").value(managerEmail));
        mockMvc.perform(get("/api/products/%d/history?page=1&size=2".formatted(productId)).header("Authorization", root))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldKeepTheHistoryPrivateFromCashiers() throws Exception {
        String root = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String cashier = login(register(root, "CASHIER"), "long-pass-123");

        mockMvc.perform(get("/api/products/1/history").header("Authorization", cashier))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/products/1/history")).andExpect(status().isUnauthorized());
    }
}
