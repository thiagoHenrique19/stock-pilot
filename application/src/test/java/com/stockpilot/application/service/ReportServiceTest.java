package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stockpilot.core.port.in.dto.DailySalesReport;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.port.out.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private SaleRepository saleRepository;

    @Test
    void shouldSummarizeDailySales() {
        Product coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 100, 3);
        Sale first = Sale.open(PaymentMethod.CASH);
        first.addItem(coffee, 1);
        Sale second = Sale.open(PaymentMethod.PIX);
        second.addItem(coffee, 3);
        when(saleRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(first, second));

        DailySalesReport report = new ReportService(saleRepository).dailySummary(LocalDate.of(2026, 11, 5));

        assertThat(report.salesCount()).isEqualTo(2);
        assertThat(report.revenue()).isEqualByComparingTo("80.00");
        assertThat(report.averageTicket()).isEqualByComparingTo("40.00");
    }

    @Test
    void shouldReturnZeroedReportWhenThereAreNoSales() {
        when(saleRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());

        DailySalesReport report = new ReportService(saleRepository).dailySummary(LocalDate.of(2026, 11, 5));

        assertThat(report.salesCount()).isZero();
        assertThat(report.averageTicket()).isEqualByComparingTo("0");
    }
}
