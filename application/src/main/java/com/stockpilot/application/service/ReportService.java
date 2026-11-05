package com.stockpilot.application.service;

import com.stockpilot.core.port.in.GenerateSalesReportUseCase;
import com.stockpilot.core.port.in.dto.DailySalesReport;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.port.out.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService implements GenerateSalesReportUseCase {

    private final SaleRepository saleRepository;

    public ReportService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DailySalesReport dailySummary(LocalDate date) {
        ZoneId zone = ZoneId.systemDefault();
        List<Sale> sales = saleRepository.findByCreatedAtBetween(
                        date.atStartOfDay(zone).toInstant(),
                        date.plusDays(1).atStartOfDay(zone).toInstant())
                .stream().filter(sale -> !sale.isCancelled()).toList();
        BigDecimal revenue = sales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sales.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);
        return new DailySalesReport(date, sales.size(), revenue, average);
    }
}
