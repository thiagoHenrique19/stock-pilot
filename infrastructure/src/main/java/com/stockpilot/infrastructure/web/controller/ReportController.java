package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.GenerateAnalyticsReportsUseCase;
import com.stockpilot.core.port.in.dto.CategoryStockValuation;
import com.stockpilot.core.port.in.dto.DailySalesReport;
import com.stockpilot.core.port.in.dto.LowStockItem;
import com.stockpilot.core.port.in.dto.PaymentMethodRevenue;
import com.stockpilot.core.port.in.dto.PeriodRevenue;
import com.stockpilot.core.port.in.dto.TopSellingProduct;
import com.stockpilot.core.query.TimeGranularity;
import com.stockpilot.core.port.in.GenerateSalesReportUseCase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final GenerateSalesReportUseCase reportService;
    private final GenerateAnalyticsReportsUseCase analyticsService;

    public ReportController(GenerateSalesReportUseCase reportService,
                            GenerateAnalyticsReportsUseCase analyticsService) {
        this.reportService = reportService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/daily")
    public DailySalesReport daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.dailySummary(date);
    }

    @GetMapping("/top-products")
    public List<TopSellingProduct> topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.topSellingProducts(from, to, limit);
    }

    @GetMapping("/revenue-by-payment-method")
    public List<PaymentMethodRevenue> revenueByPaymentMethod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.revenueByPaymentMethod(from, to);
    }

    @GetMapping("/revenue")
    public List<PeriodRevenue> revenueByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") TimeGranularity granularity) {
        return analyticsService.revenueByPeriod(from, to, granularity);
    }

    @GetMapping("/stock-valuation")
    public List<CategoryStockValuation> stockValuation() {
        return analyticsService.stockValuation();
    }

    @GetMapping("/low-stock")
    public List<LowStockItem> lowStock() {
        return analyticsService.lowStock();
    }
}
