package com.stockpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WithMockUser(roles = "ADMIN")
class StockPilotFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanSales() {
        jdbcTemplate.execute("DELETE FROM sale_item");
        jdbcTemplate.execute("DELETE FROM sale");
    }

    private int createProduct(String sku, String name, String price, int quantity, int minimum) throws Exception {
        String body = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","name":"%s","price":%s,"quantity":%d,"minimumQuantity":%d}
                                """.formatted(sku, name, price, quantity, minimum)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void shouldRunProductSaleAndReportFlow() throws Exception {
        int productId = createProduct("TEA-1", "Green tea", "10.00", 10, 3);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"PIX","discountPercentage":10,
                                 "items":[{"productId":%d,"quantity":8}]}
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(72.0));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'TEA-1')]").exists());

        mockMvc.perform(get("/api/reports/daily").param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesCount").value(1))
                .andExpect(jsonPath("$.revenue").value(72.0));
    }

    @Test
    void shouldRejectSaleWithInsufficientStock() throws Exception {
        int productId = createProduct("RICE-1", "Rice", "5.00", 1, 0);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","items":[{"productId":%d,"quantity":5}]}
                                """.formatted(productId)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectDuplicatedSku() throws Exception {
        createProduct("DUP-1", "First", "1.00", 1, 0);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"DUP-1","name":"Second","price":1.00,"quantity":1,"minimumQuantity":0}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
