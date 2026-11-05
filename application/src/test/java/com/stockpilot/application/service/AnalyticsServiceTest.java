package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.port.in.dto.TopSellingProduct;
import com.stockpilot.core.port.out.SalesAnalyticsRepository;
import com.stockpilot.core.query.TimeGranularity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private SalesAnalyticsRepository repository;
    @InjectMocks private AnalyticsService service;

    private final LocalDate from = LocalDate.of(2026, 11, 1);
    private final LocalDate to = LocalDate.of(2026, 11, 30);
    private final ZoneId zone = ZoneId.systemDefault();

    @Test
    void shouldConvertInclusiveDatesToInstantRangeForTopProducts() {
        List<TopSellingProduct> expected = List.of(new TopSellingProduct(1, 1L, "C-1", "Coffee", 8, new BigDecimal("160.00")));
        when(repository.topSellingProducts(from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant(), 5)).thenReturn(expected);

        assertThat(service.topSellingProducts(from, to, 5)).isEqualTo(expected);
    }

    @Test
    void shouldRejectInvalidLimit() {
        assertThatThrownBy(() -> service.topSellingProducts(from, to, 0)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.topSellingProducts(from, to, 101)).isInstanceOf(InvalidRequestException.class);
        verify(repository, never()).topSellingProducts(any(), any(), eq(0));
    }

    @Test
    void shouldRejectInvertedRange() {
        assertThatThrownBy(() -> service.revenueByPaymentMethod(to, from)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.revenueByPeriod(to, from, TimeGranularity.DAY))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldRejectMissingDates() {
        assertThatThrownBy(() -> service.revenueByPaymentMethod(null, to)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.revenueByPeriod(from, null, TimeGranularity.DAY))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldRejectMissingGranularity() {
        assertThatThrownBy(() -> service.revenueByPeriod(from, to, null)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldGroupRevenueUsingSystemZone() {
        service.revenueByPeriod(from, to, TimeGranularity.WEEK);

        verify(repository).revenueByPeriod(from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant(), TimeGranularity.WEEK, zone);
    }

    @Test
    void shouldDelegateStockReports() {
        service.stockValuation();
        service.lowStock();

        verify(repository).stockValuation();
        verify(repository).lowStock();
    }
}
