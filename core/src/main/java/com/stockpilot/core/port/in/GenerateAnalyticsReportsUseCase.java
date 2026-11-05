package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.CategoryStockValuation;
import com.stockpilot.core.port.in.dto.LowStockItem;
import com.stockpilot.core.port.in.dto.PaymentMethodRevenue;
import com.stockpilot.core.port.in.dto.PeriodRevenue;
import com.stockpilot.core.port.in.dto.TopSellingProduct;
import com.stockpilot.core.query.TimeGranularity;
import java.time.LocalDate;
import java.util.List;

/** Analytics reports. Date ranges are inclusive on both ends. */
public interface GenerateAnalyticsReportsUseCase {

    List<TopSellingProduct> topSellingProducts(LocalDate from, LocalDate to, int limit);

    List<PaymentMethodRevenue> revenueByPaymentMethod(LocalDate from, LocalDate to);

    List<PeriodRevenue> revenueByPeriod(LocalDate from, LocalDate to, TimeGranularity granularity);

    List<CategoryStockValuation> stockValuation();

    List<LowStockItem> lowStock();
}
