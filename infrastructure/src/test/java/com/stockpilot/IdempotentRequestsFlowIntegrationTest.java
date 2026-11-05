package com.stockpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WithMockUser(roles = "ADMIN")
class IdempotentRequestsFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private int createProduct(int quantity) throws Exception {
        String body = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"ID-%s\",\"name\":\"Idem\",\"price\":10.00,\"quantity\":%d,\"minimumQuantity\":0}"
                                .formatted(UUID.randomUUID().toString().substring(0, 8), quantity)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions sell(int productId, int quantity, String key) throws Exception {
        var request = post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentMethod\":\"PIX\",\"items\":[{\"productId\":%d,\"quantity\":%d}]}"
                        .formatted(productId, quantity));
        return mockMvc.perform(key == null ? request : request.header("Idempotency-Key", key));
    }

    private int quantity(int productId) {
        return jdbc.queryForObject("SELECT quantity FROM product WHERE id = ?", Integer.class, productId);
    }

    private int salesOf(int productId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sale_item WHERE product_id = ?", Integer.class, productId);
    }

    @Test
    void shouldReturnTheOriginalSaleWhenTheSameRequestIsRetried() throws Exception {
        int productId = createProduct(10);
        String key = UUID.randomUUID().toString();

        String first = sell(productId, 3, key).andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andReturn().getResponse().getContentAsString();
        String second = sell(productId, 3, key).andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat((Integer) JsonPath.read(second, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        assertThat((String) JsonPath.read(second, "$.receiptNumber")).isEqualTo(JsonPath.read(first, "$.receiptNumber"));
        assertThat((Object) JsonPath.read(second, "$.total")).isEqualTo(JsonPath.read(first, "$.total"));
        assertThat(salesOf(productId)).isEqualTo(1);
        assertThat(quantity(productId)).isEqualTo(7);
    }

    @Test
    void shouldAnswer422WhenTheKeyIsReusedWithADifferentBody() throws Exception {
        int productId = createProduct(10);
        String key = UUID.randomUUID().toString();
        sell(productId, 1, key).andExpect(status().isCreated());

        sell(productId, 2, key).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Idempotency-Key was already used with a different request"));

        assertThat(quantity(productId)).isEqualTo(9);
    }

    @Test
    void shouldLetTheClientRetryTheSameKeyAfterABusinessFailure() throws Exception {
        int productId = createProduct(1);
        String key = UUID.randomUUID().toString();

        sell(productId, 2, key).andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_key WHERE key = ?", Integer.class, key)).isZero();
        mockMvc.perform(post("/api/products/%d/restock".formatted(productId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":5}")).andExpect(status().isOk());

        sell(productId, 2, key).andExpect(status().isCreated());
        assertThat(quantity(productId)).isEqualTo(4);
    }

    @Test
    void shouldTreatTheSameKeyOnDifferentOperationsAsDifferentRequests() throws Exception {
        int first = createProduct(5);
        int second = createProduct(5);
        String key = UUID.randomUUID().toString();
        String restock = "{\"quantity\":2}";

        mockMvc.perform(post("/api/products/%d/restock".formatted(first)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(restock)).andExpect(status().isOk());
        mockMvc.perform(post("/api/products/%d/restock".formatted(second)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(restock)).andExpect(status().isOk())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));

        assertThat(quantity(first)).isEqualTo(7);
        assertThat(quantity(second)).isEqualTo(7);
    }

    @Test
    void shouldApplyARestockOnlyOnceWhenRetried() throws Exception {
        int productId = createProduct(5);
        String key = UUID.randomUUID().toString();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/products/%d/restock".formatted(productId)).header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":4}")).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(9));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE product_id = ? AND reason = 'Restock'",
                Integer.class, productId)).isEqualTo(1);
    }

    @Test
    void shouldApplyAnAdjustmentOnlyOnceWhenRetried() throws Exception {
        int productId = createProduct(5);
        String key = UUID.randomUUID().toString();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/products/%d/adjustments".formatted(productId)).header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-2,\"reason\":\"Damaged\"}"))
                    .andExpect(status().isOk());
        }

        assertThat(quantity(productId)).isEqualTo(3);
    }

    @Test
    void shouldRejectAMalformedKeyWithoutCreatingAnything() throws Exception {
        int productId = createProduct(5);

        sell(productId, 1, "has space").andExpect(status().isBadRequest());

        assertThat(salesOf(productId)).isZero();
    }

    @Test
    void shouldKeepWorkingWithoutAKey() throws Exception {
        int productId = createProduct(5);

        sell(productId, 1, null).andExpect(status().isCreated());
        sell(productId, 1, null).andExpect(status().isCreated());

        assertThat(salesOf(productId)).isEqualTo(2);
    }
}
