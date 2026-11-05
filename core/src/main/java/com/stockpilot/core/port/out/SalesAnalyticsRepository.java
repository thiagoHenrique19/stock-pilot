package com.stockpilot.core.port.out;

import com.stockpilot.core.port.in.dto.CategoryStockValuation;
import com.stockpilot.core.port.in.dto.LowStockItem;
import com.stockpilot.core.port.in.dto.PaymentMethodRevenue;
import com.stockpilot.core.port.in.dto.PeriodRevenue;
import com.stockpilot.core.port.in.dto.TopSellingProduct;
import com.stockpilot.core.query.TimeGranularity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Read-only reporting queries. Cancelled sales are always excluded. {@code from} is inclusive and
 * {@code to} is exclusive.
 */
public interface SalesAnalyticsRepository {

    List<TopSellingProduct> topSellingProducts(Instant from, Instant to, int limit);

    List<PaymentMethodRevenue> revenueByPaymentMethod(Instant from, Instant to);

    List<PeriodRevenue> revenueByPeriod(Instant from, Instant to, TimeGranularity granularity, ZoneId zone);

    List<CategoryStockValuation> stockValuation();

    List<LowStockItem> lowStock();
}
