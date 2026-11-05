package com.stockpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class StockMovementFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private int createProduct(int quantity) throws Exception {
        String body = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"LED-%s","name":"Ledger item","price":10.00,"quantity":%d,"minimumQuantity":1}
                                """.formatted(UUID.randomUUID().toString().substring(0, 8), quantity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void shouldRecordEveryStockChangeInTheLedger() throws Exception {
        int id = createProduct(10);

        mockMvc.perform(post("/api/products/%d/restock".formatted(id)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":5}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"PIX\",\"items\":[{\"productId\":%d,\"quantity\":3}]}".formatted(id)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/products/%d/adjustments".formatted(id)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-2,\"reason\":\"Broken packages\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10));

        mockMvc.perform(get("/api/products/%d/movements".formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[0].quantity").value(-2))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(10))
                .andExpect(jsonPath("$.content[0].reason").value("Broken packages"))
                .andExpect(jsonPath("$.content[1].type").value("OUT"))
                .andExpect(jsonPath("$.content[1].quantity").value(-3))
                .andExpect(jsonPath("$.content[1].balanceAfter").value(12))
                .andExpect(jsonPath("$.content[1].reference").value(org.hamcrest.Matchers.startsWith("SALE-")))
                .andExpect(jsonPath("$.content[2].type").value("IN"))
                .andExpect(jsonPath("$.content[2].balanceAfter").value(15))
                .andExpect(jsonPath("$.content[3].balanceAfter").value(10));
    }

    @Test
    void shouldRejectAdjustmentBelowZeroAndLeaveStockUntouched() throws Exception {
        int id = createProduct(2);

        mockMvc.perform(post("/api/products/%d/adjustments".formatted(id)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-3,\"reason\":\"Loss\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.quantity").value(2));
        mockMvc.perform(get("/api/products/%d/movements".formatted(id)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldPaginateMovementsAndReturnNotFoundForUnknownProduct() throws Exception {
        int id = createProduct(1);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/products/%d/adjustments".formatted(id)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":1,\"reason\":\"Count %d\"}".formatted(i))).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/products/%d/movements".formatted(id)).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/products/999999/movements")).andExpect(status().isNotFound());
    }
}
