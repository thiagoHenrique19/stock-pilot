package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.PaymentMethod;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SalesAnalyticsIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final Instant FROM = LocalDate.of(2026, 11, 1).atStartOfDay(ZONE).toInstant();
    private static final Instant TO = LocalDate.of(2026, 12, 1).atStartOfDay(ZONE).toInstant();

    @Autowired private SalesAnalyticsRepository analytics;
    @Autowired private JdbcTemplate jdbc;

    private long coffee;
    private long tea;
    private long rice;

    @BeforeEach
    void seed() {
        jdbc.execute("DELETE FROM stock_movement");
        jdbc.execute("DELETE FROM sale_item");
        jdbc.execute("DELETE FROM sale");
        jdbc.execute("DELETE FROM product");
        jdbc.execute("DELETE FROM category");

        long drinks = jdbc.queryForObject("INSERT INTO category (name) VALUES ('Drinks') RETURNING id", Long.class);
        coffee = product("COFFEE", "Coffee", "20.00", 10, 3, drinks, true);
        tea = product("TEA", "Tea", "5.00", 2, 5, drinks, true);
        rice = product("RICE", "Rice", "10.00", 100, 10, null, true);
        product("OLD", "Discontinued", "1.00", 0, 5, null, false);

        long s1 = sale("2026-11-02", "PIX", "10.00", "COMPLETED");
        item(s1, coffee, 2, "20.00");
        item(s1, tea, 4, "5.00");
        long s2 = sale("2026-11-02", "CASH", "0.00", "COMPLETED");
        item(s2, coffee, 1, "20.00");
        long s3 = sale("2026-11-03", "PIX", "0.00", "COMPLETED");
        item(s3, rice, 3, "10.00");
        long s4 = sale("2026-11-10", "CREDIT_CARD", "0.00", "COMPLETED");
        item(s4, coffee, 5, "20.00");
        long cancelled = sale("2026-11-02", "PIX", "0.00", "CANCELLED");
        item(cancelled, coffee, 100, "20.00");
        long outside = sale("2026-10-15", "PIX", "0.00", "COMPLETED");
        item(outside, rice, 1, "10.00");
    }

    private long product(String sku, String name, String price, int quantity, int minimum, Long category, boolean active) {
        return jdbc.queryForObject("""
                INSERT INTO product (sku, name, price, quantity, minimum_quantity, category_id, active)
                VALUES (?, ?, ?::numeric, ?, ?, ?, ?) RETURNING id
                """, Long.class, sku, name, price, quantity, minimum, category, active);
    }

    private long sale(String date, String method, String discount, String status) {
        Instant createdAt = LocalDate.parse(date).atTime(12, 0).atZone(ZONE).toInstant();
        return jdbc.queryForObject("""
                INSERT INTO sale (payment_method, discount_amount, created_at, status, receipt_number)
                VALUES (?, ?::numeric, ?, ?, 'T-' || nextval('sale_receipt_seq')) RETURNING id
                """, Long.class, method, discount, java.sql.Timestamp.from(createdAt), status);
    }

    private void item(long saleId, long productId, int quantity, String unitPrice) {
        jdbc.update("INSERT INTO sale_item (sale_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?::numeric)",
                saleId, productId, quantity, unitPrice);
    }

    @Test
    void shouldRankTopSellingProductsIgnoringCancelledAndOutOfRangeSales() {
        List<TopSellingProduct> top = analytics.topSellingProducts(FROM, TO, 10);

        assertThat(top).extracting(TopSellingProduct::name).containsExactly("Coffee", "Tea", "Rice");
        assertThat(top).extracting(TopSellingProduct::rank).containsExactly(1, 2, 3);
        assertThat(top).extracting(TopSellingProduct::unitsSold).containsExactly(8L, 4L, 3L);
        assertThat(top.get(0).revenue()).isEqualByComparingTo("160.00");
        assertThat(top.get(0).productId()).isEqualTo(coffee);
        assertThat(top.get(1).productId()).isEqualTo(tea);
        assertThat(top.get(2).productId()).isEqualTo(rice);
    }

    @Test
    void shouldApplyLimitToTopProducts() {
        assertThat(analytics.topSellingProducts(FROM, TO, 2)).extracting(TopSellingProduct::name)
                .containsExactly("Coffee", "Tea");
    }

    @Test
    void shouldCalculateRevenueByPaymentMethodAfterDiscounts() {
        List<PaymentMethodRevenue> revenue = analytics.revenueByPaymentMethod(FROM, TO);

        assertThat(revenue).extracting(PaymentMethodRevenue::paymentMethod)
                .containsExactly(PaymentMethod.CREDIT_CARD, PaymentMethod.PIX, PaymentMethod.CASH);
        assertThat(revenue).extracting(PaymentMethodRevenue::salesCount).containsExactly(1L, 2L, 1L);
        assertThat(revenue.get(0).revenue()).isEqualByComparingTo("100.00");
        assertThat(revenue.get(1).revenue()).isEqualByComparingTo("80.00");
        assertThat(revenue.get(2).revenue()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldGroupRevenueByDayWithCumulativeTotal() {
        List<PeriodRevenue> days = analytics.revenueByPeriod(FROM, TO, TimeGranularity.DAY, ZONE);

        assertThat(days).extracting(PeriodRevenue::periodStart)
                .containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 10));
        assertThat(days).extracting(PeriodRevenue::salesCount).containsExactly(2L, 1L, 1L);
        assertThat(days).extracting(PeriodRevenue::revenue)
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactly(new java.math.BigDecimal("70.00"), new java.math.BigDecimal("30.00"),
                        new java.math.BigDecimal("100.00"));
        assertThat(days).extracting(PeriodRevenue::cumulativeRevenue)
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactly(new java.math.BigDecimal("70.00"), new java.math.BigDecimal("100.00"),
                        new java.math.BigDecimal("200.00"));
    }

    @Test
    void shouldGroupRevenueByWeekStartingOnMonday() {
        List<PeriodRevenue> weeks = analytics.revenueByPeriod(FROM, TO, TimeGranularity.WEEK, ZONE);

        assertThat(weeks).extracting(PeriodRevenue::periodStart)
                .containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9));
        assertThat(weeks).extracting(PeriodRevenue::salesCount).containsExactly(3L, 1L);
        assertThat(weeks.get(0).revenue()).isEqualByComparingTo("100.00");
        assertThat(weeks.get(1).cumulativeRevenue()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldGroupRevenueByMonth() {
        List<PeriodRevenue> months = analytics.revenueByPeriod(
                LocalDate.of(2026, 10, 1).atStartOfDay(ZONE).toInstant(), TO, TimeGranularity.MONTH, ZONE);

        assertThat(months).extracting(PeriodRevenue::periodStart)
                .containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
        assertThat(months).extracting(PeriodRevenue::salesCount).containsExactly(1L, 4L);
        assertThat(months.get(1).revenue()).isEqualByComparingTo("200.00");
        assertThat(months.get(1).cumulativeRevenue()).isEqualByComparingTo("210.00");
    }

    @Test
    void shouldReturnEmptyResultsOutsideTheSalesPeriod() {
        Instant later = LocalDate.of(2027, 1, 1).atStartOfDay(ZONE).toInstant();
        Instant evenLater = LocalDate.of(2027, 2, 1).atStartOfDay(ZONE).toInstant();

        assertThat(analytics.topSellingProducts(later, evenLater, 5)).isEmpty();
        assertThat(analytics.revenueByPaymentMethod(later, evenLater)).isEmpty();
        assertThat(analytics.revenueByPeriod(later, evenLater, TimeGranularity.DAY, ZONE)).isEmpty();
    }

    @Test
    void shouldValueStockPerCategoryIncludingUncategorizedProducts() {
        List<CategoryStockValuation> valuation = analytics.stockValuation();

        assertThat(valuation).extracting(CategoryStockValuation::categoryName)
                .containsExactly("Uncategorized", "Drinks");
        assertThat(valuation.get(0).totalValue()).isEqualByComparingTo("1000.00");
        assertThat(valuation.get(0).totalUnits()).isEqualTo(100);
        assertThat(valuation.get(0).categoryId()).isNull();
        assertThat(valuation.get(1).totalValue()).isEqualByComparingTo("210.00");
        assertThat(valuation.get(1).totalUnits()).isEqualTo(12);
    }

    @Test
    void shouldListActiveLowStockProductsWithShortfall() {
        List<LowStockItem> low = analytics.lowStock();

        assertThat(low).hasSize(1);
        assertThat(low.get(0).name()).isEqualTo("Tea");
        assertThat(low.get(0).shortfall()).isEqualTo(3);
        assertThat(low.get(0).quantity()).isEqualTo(2);
        assertThat(low.get(0).minimumQuantity()).isEqualTo(5);
    }
}
