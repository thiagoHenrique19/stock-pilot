package com.stockpilot;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class SaleManagementFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanSales() {
        jdbcTemplate.execute("DELETE FROM sale_item");
        jdbcTemplate.execute("DELETE FROM sale");
    }

    private int createProduct(int quantity) throws Exception {
        String body = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SM-%s","name":"Managed","price":20.00,"quantity":%d,"minimumQuantity":1}
                                """.formatted(UUID.randomUUID().toString().substring(0, 8), quantity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int sell(int productId, int quantity, String extra) throws Exception {
        String body = mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{%s\"items\":[{\"productId\":%d,\"quantity\":%d}]}".formatted(extra, productId, quantity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void shouldCancelSaleRestoreStockAndRejectSecondCancellation() throws Exception {
        int productId = createProduct(10);
        int saleId = sell(productId, 4, "\"paymentMethod\":\"PIX\",");

        mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(6));

        mockMvc.perform(post("/api/sales/%d/cancel".formatted(saleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());
        mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(10));
        mockMvc.perform(get("/api/products/%d/movements".formatted(productId)))
                .andExpect(jsonPath("$.content[0].type").value("IN"))
                .andExpect(jsonPath("$.content[0].reason").value("Sale cancelled"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(10));

        mockMvc.perform(post("/api/sales/%d/cancel".formatted(saleId))).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(10));
        mockMvc.perform(post("/api/sales/999999/cancel")).andExpect(status().isNotFound());
    }

    @Test
    void shouldGenerateSequentialReceiptNumbersAndGetSaleById() throws Exception {
        int productId = createProduct(10);
        int first = sell(productId, 1, "\"paymentMethod\":\"PIX\",");
        int second = sell(productId, 1, "\"paymentMethod\":\"PIX\",");

        String firstReceipt = JsonPath.read(mockMvc.perform(get("/api/sales/" + first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.receiptNumber", matchesPattern("SALE-\\d{6}")))
                .andReturn().getResponse().getContentAsString(), "$.receiptNumber");
        String secondReceipt = JsonPath.read(mockMvc.perform(get("/api/sales/" + second))
                .andReturn().getResponse().getContentAsString(), "$.receiptNumber");

        org.assertj.core.api.Assertions.assertThat(Long.parseLong(secondReceipt.substring(5)))
                .isEqualTo(Long.parseLong(firstReceipt.substring(5)) + 1);
        mockMvc.perform(get("/api/sales/999999")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnChangeForCashAndRejectInsufficientAmount() throws Exception {
        int productId = createProduct(10);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","amountReceived":100.00,
                                 "items":[{"productId":%d,"quantity":3}]}
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(60.0))
                .andExpect(jsonPath("$.change").value(40.0));

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","amountReceived":10.00,
                                 "items":[{"productId":%d,"quantity":3}]}
                                """.formatted(productId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"PIX","amountReceived":100.00,
                                 "items":[{"productId":%d,"quantity":1}]}
                                """.formatted(productId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(7));
    }

    @Test
    void shouldApplyFixedAmountDiscountAndValidateIt() throws Exception {
        int productId = createProduct(10);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"PIX\",\"discountAmount\":15.00,\"items\":[{\"productId\":%d,\"quantity\":2}]}"
                                .formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(15.0))
                .andExpect(jsonPath("$.total").value(25.0));

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"PIX\",\"discountAmount\":41.00,\"items\":[{\"productId\":%d,\"quantity\":2}]}"
                                .formatted(productId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"PIX\",\"discountAmount\":5.00,\"discountPercentage\":10,\"items\":[{\"productId\":%d,\"quantity\":2}]}"
                                .formatted(productId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(8));
    }

    @Test
    void shouldListSalesWithFiltersAndPagination() throws Exception {
        int productId = createProduct(50);
        int pix = sell(productId, 1, "\"paymentMethod\":\"PIX\",");
        sell(productId, 1, "\"paymentMethod\":\"PIX\",");
        sell(productId, 1, "\"paymentMethod\":\"DEBIT_CARD\",");
        mockMvc.perform(post("/api/sales/%d/cancel".formatted(pix))).andExpect(status().isOk());
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/sales").param("from", today).param("to", today))
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get("/api/sales").param("paymentMethod", "PIX").param("status", "COMPLETED"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/sales").param("status", "CANCELLED"))
                .andExpect(jsonPath("$.content[0].id").value(pix));
        mockMvc.perform(get("/api/sales").param("from", LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/sales").param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldExcludeCancelledSalesFromDailyReport() throws Exception {
        int productId = createProduct(10);
        sell(productId, 1, "\"paymentMethod\":\"PIX\",");
        int cancelled = sell(productId, 2, "\"paymentMethod\":\"PIX\",");
        mockMvc.perform(post("/api/sales/%d/cancel".formatted(cancelled))).andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/daily").param("date", LocalDate.now().toString()))
                .andExpect(jsonPath("$.salesCount").value(1))
                .andExpect(jsonPath("$.revenue").value(20.0));
    }
}
