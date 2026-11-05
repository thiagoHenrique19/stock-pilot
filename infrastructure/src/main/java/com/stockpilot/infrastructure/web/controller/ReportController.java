package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.dto.DailySalesReport;
import com.stockpilot.core.port.in.GenerateSalesReportUseCase;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final GenerateSalesReportUseCase reportService;

    public ReportController(GenerateSalesReportUseCase reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily")
    public DailySalesReport daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.dailySummary(date);
    }
}
