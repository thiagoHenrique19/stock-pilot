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
import org.springframework.test.web.servlet.MockMvc;

class AnalyticsReportsFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("DELETE FROM stock_movement");
        jdbc.execute("DELETE FROM sale_item");
        jdbc.execute("DELETE FROM sale");
        jdbc.execute("DELETE FROM product");
        jdbc.execute("DELETE FROM category");
    }

    private int createCategory(String name) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\"}".formatted(name))).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private int createProduct(String sku, String name, String price, int quantity, int minimum, int categoryId)
            throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku":"%s","name":"%s","price":%s,"quantity":%d,"minimumQuantity":%d,"categoryId":%d}
                        """.formatted(sku, name, price, quantity, minimum, categoryId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private int sell(int productId, int quantity, String method) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentMethod\":\"%s\",\"items\":[{\"productId\":%d,\"quantity\":%d}]}"
                        .formatted(method, productId, quantity)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void shouldReportOnSalesCreatedThroughTheApi() throws Exception {
        int category = createCategory("Hot drinks");
        int coffee = createProduct("COF", "Coffee", "20.00", 20, 17, category);
        int tea = createProduct("TEA", "Tea", "5.00", 50, 2, category);
        sell(coffee, 3, "PIX");
        sell(tea, 10, "CASH");
        int cancelled = sell(coffee, 10, "PIX");
        mockMvc.perform(post("/api/sales/%d/cancel".formatted(cancelled))).andExpect(status().isOk());
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/reports/top-products").param("from", today).param("to", today).param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Tea"))
                .andExpect(jsonPath("$[0].unitsSold").value(10))
                .andExpect(jsonPath("$[0].rank").value(1));
        mockMvc.perform(get("/api/reports/revenue-by-payment-method").param("from", today).param("to", today))
                .andExpect(jsonPath("$[0].paymentMethod").value("PIX"))
                .andExpect(jsonPath("$[0].revenue").value(60.0))
                .andExpect(jsonPath("$[1].paymentMethod").value("CASH"))
                .andExpect(jsonPath("$[1].revenue").value(50.0));
        mockMvc.perform(get("/api/reports/revenue").param("from", today).param("to", today).param("granularity", "MONTH"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].salesCount").value(2))
                .andExpect(jsonPath("$[0].revenue").value(110.0))
                .andExpect(jsonPath("$[0].periodStart").value(today.substring(0, 8) + "01"));
        mockMvc.perform(get("/api/reports/stock-valuation"))
                .andExpect(jsonPath("$[0].categoryName").value("Hot drinks"))
                .andExpect(jsonPath("$[0].totalValue").value(20 * 17 * 1.0 + 5 * 40));
        mockMvc.perform(get("/api/reports/low-stock"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sku").value("COF"));
        mockMvc.perform(get("/api/reports/daily").param("date", today))
                .andExpect(jsonPath("$.salesCount").value(2));
    }

    @Test
    void shouldValidateReportParameters() throws Exception {
        mockMvc.perform(get("/api/reports/top-products").param("from", "2026-11-30").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports/top-products")
                        .param("from", "2026-11-01").param("to", "2026-11-30").param("limit", "1000"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports/revenue").param("from", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }
}
