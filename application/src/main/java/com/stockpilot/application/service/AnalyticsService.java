package com.stockpilot.application.service;

import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.port.in.GenerateAnalyticsReportsUseCase;
import com.stockpilot.core.port.in.dto.CategoryStockValuation;
import com.stockpilot.core.port.in.dto.LowStockItem;
import com.stockpilot.core.port.in.dto.PaymentMethodRevenue;
import com.stockpilot.core.port.in.dto.PeriodRevenue;
import com.stockpilot.core.port.in.dto.TopSellingProduct;
import com.stockpilot.core.port.out.SalesAnalyticsRepository;
import com.stockpilot.core.query.TimeGranularity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService implements GenerateAnalyticsReportsUseCase {

    static final int MAX_TOP_PRODUCTS = 100;

    private final SalesAnalyticsRepository repository;

    public AnalyticsService(SalesAnalyticsRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TopSellingProduct> topSellingProducts(LocalDate from, LocalDate to, int limit) {
        if (limit < 1 || limit > MAX_TOP_PRODUCTS) {
            throw new InvalidRequestException("Limit must be between 1 and " + MAX_TOP_PRODUCTS);
        }
        validateRange(from, to);
        return repository.topSellingProducts(start(from), endExclusive(to), limit);
    }

    @Override
    public List<PaymentMethodRevenue> revenueByPaymentMethod(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return repository.revenueByPaymentMethod(start(from), endExclusive(to));
    }

    @Override
    public List<PeriodRevenue> revenueByPeriod(LocalDate from, LocalDate to, TimeGranularity granularity) {
        validateRange(from, to);
        if (granularity == null) {
            throw new InvalidRequestException("Granularity is required (DAY, WEEK or MONTH)");
        }
        return repository.revenueByPeriod(start(from), endExclusive(to), granularity, ZoneId.systemDefault());
    }

    @Override
    public List<CategoryStockValuation> stockValuation() {
        return repository.stockValuation();
    }

    @Override
    public List<LowStockItem> lowStock() {
        return repository.lowStock();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidRequestException("Both 'from' and 'to' dates are required");
        }
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' date must not be after 'to' date");
        }
    }

    private Instant start(LocalDate from) {
        return from.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant endExclusive(LocalDate to) {
        return to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
