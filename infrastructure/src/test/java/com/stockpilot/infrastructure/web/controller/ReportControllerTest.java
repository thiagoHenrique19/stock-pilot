package com.stockpilot.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.port.in.GenerateAnalyticsReportsUseCase;
import com.stockpilot.core.port.in.GenerateSalesReportUseCase;
import com.stockpilot.core.port.in.dto.CategoryStockValuation;
import com.stockpilot.core.port.in.dto.DailySalesReport;
import com.stockpilot.core.port.in.dto.LowStockItem;
import com.stockpilot.core.port.in.dto.PaymentMethodRevenue;
import com.stockpilot.core.port.in.dto.PeriodRevenue;
import com.stockpilot.core.port.in.dto.TopSellingProduct;
import com.stockpilot.core.query.TimeGranularity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.stockpilot.infrastructure.web.support.WithSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@WithSecurityConfig
@WithMockUser(roles = "ADMIN")
class ReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private GenerateSalesReportUseCase reportService;
    @MockBean private GenerateAnalyticsReportsUseCase analyticsService;

    private final LocalDate from = LocalDate.of(2026, 11, 1);
    private final LocalDate to = LocalDate.of(2026, 11, 30);

    @Test
    void shouldKeepDailyReportWorking() throws Exception {
        when(reportService.dailySummary(from)).thenReturn(
                new DailySalesReport(from, 2, new BigDecimal("80.00"), new BigDecimal("40.00")));

        mockMvc.perform(get("/api/reports/daily").param("date", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesCount").value(2));
    }

    @Test
    void shouldReturnTopProductsUsingDefaultLimit() throws Exception {
        when(analyticsService.topSellingProducts(from, to, 10))
                .thenReturn(List.of(new TopSellingProduct(1, 1L, "C-1", "Coffee", 8, new BigDecimal("160.00"))));

        mockMvc.perform(get("/api/reports/top-products").param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Coffee"))
                .andExpect(jsonPath("$[0].unitsSold").value(8));
    }

    @Test
    void shouldPassExplicitLimit() throws Exception {
        mockMvc.perform(get("/api/reports/top-products")
                .param("from", "2026-11-01").param("to", "2026-11-30").param("limit", "3")).andExpect(status().isOk());

        verify(analyticsService).topSellingProducts(from, to, 3);
    }

    @Test
    void shouldReturnRevenueByPaymentMethod() throws Exception {
        when(analyticsService.revenueByPaymentMethod(from, to))
                .thenReturn(List.of(new PaymentMethodRevenue(PaymentMethod.PIX, 2, new BigDecimal("80.00"))));

        mockMvc.perform(get("/api/reports/revenue-by-payment-method").param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentMethod").value("PIX"))
                .andExpect(jsonPath("$[0].revenue").value(80.0));
    }

    @Test
    void shouldReturnRevenueByPeriod() throws Exception {
        when(analyticsService.revenueByPeriod(from, to, TimeGranularity.WEEK)).thenReturn(List.of(
                new PeriodRevenue(LocalDate.of(2026, 11, 2), 3, new BigDecimal("100.00"), new BigDecimal("100.00"))));

        mockMvc.perform(get("/api/reports/revenue")
                        .param("from", "2026-11-01").param("to", "2026-11-30").param("granularity", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].periodStart").value("2026-11-02"))
                .andExpect(jsonPath("$[0].salesCount").value(3));
    }

    @Test
    void shouldDefaultGranularityToDay() throws Exception {
        mockMvc.perform(get("/api/reports/revenue").param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isOk());

        verify(analyticsService).revenueByPeriod(from, to, TimeGranularity.DAY);
    }

    @Test
    void shouldReturnStockValuationAndLowStock() throws Exception {
        when(analyticsService.stockValuation())
                .thenReturn(List.of(new CategoryStockValuation(1L, "Drinks", 12, new BigDecimal("210.00"))));
        when(analyticsService.lowStock()).thenReturn(List.of(new LowStockItem(2L, "TEA", "Tea", 2, 5, 3)));

        mockMvc.perform(get("/api/reports/stock-valuation"))
                .andExpect(jsonPath("$[0].categoryName").value("Drinks"))
                .andExpect(jsonPath("$[0].totalValue").value(210.0));
        mockMvc.perform(get("/api/reports/low-stock"))
                .andExpect(jsonPath("$[0].shortfall").value(3));
    }

    @Test
    void shouldRejectMissingMalformedAndInvalidParameters() throws Exception {
        mockMvc.perform(get("/api/reports/top-products").param("from", "2026-11-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports/revenue").param("from", "yesterday").param("to", "2026-11-30"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports/revenue")
                        .param("from", "2026-11-01").param("to", "2026-11-30").param("granularity", "YEAR"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports/top-products")
                        .param("from", "2026-11-01").param("to", "2026-11-30").param("limit", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMapServiceValidationFailuresToBadRequest() throws Exception {
        when(analyticsService.topSellingProducts(eq(from), eq(to), eq(500)))
                .thenThrow(new InvalidRequestException("Limit must be between 1 and 100"));

        mockMvc.perform(get("/api/reports/top-products")
                        .param("from", "2026-11-01").param("to", "2026-11-30").param("limit", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Limit must be between 1 and 100"));
    }
}
