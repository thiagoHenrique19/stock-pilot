package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.port.in.dto.CategoryStockValuation;
import com.stockpilot.core.port.in.dto.LowStockItem;
import com.stockpilot.core.port.in.dto.PaymentMethodRevenue;
import com.stockpilot.core.port.in.dto.PeriodRevenue;
import com.stockpilot.core.port.in.dto.TopSellingProduct;
import com.stockpilot.core.port.out.SalesAnalyticsRepository;
import com.stockpilot.core.query.TimeGranularity;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL implementation of the reporting queries. The sale total is the sum of its items minus
 * the sale discount; product revenue is the sum of item subtotals (before sale level discounts).
 */
@Repository
class JdbcSalesAnalyticsRepository implements SalesAnalyticsRepository {

    private static final String SALE_TOTALS = """
            SELECT s.id, s.payment_method, s.created_at,
                   COALESCE(SUM(si.quantity * si.unit_price), 0) - s.discount_amount AS total
            FROM sale s
            LEFT JOIN sale_item si ON si.sale_id = s.id
            WHERE s.status = 'COMPLETED' AND s.created_at >= :from AND s.created_at < :to
            GROUP BY s.id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    JdbcSalesAnalyticsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TopSellingProduct> topSellingProducts(Instant from, Instant to, int limit) {
        String sql = """
                SELECT RANK() OVER (ORDER BY SUM(si.quantity) DESC, p.id) AS rank,
                       p.id, p.sku, p.name,
                       SUM(si.quantity) AS units_sold,
                       SUM(si.quantity * si.unit_price) AS revenue
                FROM sale_item si
                JOIN sale s ON s.id = si.sale_id
                JOIN product p ON p.id = si.product_id
                WHERE s.status = 'COMPLETED' AND s.created_at >= :from AND s.created_at < :to
                GROUP BY p.id, p.sku, p.name
                ORDER BY units_sold DESC, p.id
                LIMIT :limit
                """;
        return jdbc.query(sql, range(from, to).addValue("limit", limit),
                (rs, i) -> new TopSellingProduct(rs.getInt("rank"), rs.getLong("id"), rs.getString("sku"),
                        rs.getString("name"), rs.getLong("units_sold"), rs.getBigDecimal("revenue")));
    }

    @Override
    public List<PaymentMethodRevenue> revenueByPaymentMethod(Instant from, Instant to) {
        String sql = "SELECT payment_method, COUNT(*) AS sales_count, SUM(total) AS revenue FROM ("
                + SALE_TOTALS + ") t GROUP BY payment_method ORDER BY revenue DESC, payment_method";
        return jdbc.query(sql, range(from, to),
                (rs, i) -> new PaymentMethodRevenue(PaymentMethod.valueOf(rs.getString("payment_method")),
                        rs.getLong("sales_count"), rs.getBigDecimal("revenue")));
    }

    @Override
    public List<PeriodRevenue> revenueByPeriod(Instant from, Instant to, TimeGranularity granularity, ZoneId zone) {
        String sql = """
                SELECT period_start, sales_count, revenue,
                       SUM(revenue) OVER (ORDER BY period_start) AS cumulative_revenue
                FROM (
                    SELECT date_trunc(:unit, created_at AT TIME ZONE :zone)::date AS period_start,
                           COUNT(*) AS sales_count,
                           SUM(total) AS revenue
                    FROM (""" + SALE_TOTALS + """
                    ) t
                    GROUP BY 1
                ) grouped
                ORDER BY period_start
                """;
        MapSqlParameterSource params = range(from, to)
                .addValue("unit", granularity.name().toLowerCase(Locale.ROOT))
                .addValue("zone", zone.getId());
        return jdbc.query(sql, params,
                (rs, i) -> new PeriodRevenue(rs.getDate("period_start").toLocalDate(), rs.getLong("sales_count"),
                        rs.getBigDecimal("revenue"), rs.getBigDecimal("cumulative_revenue")));
    }

    @Override
    public List<CategoryStockValuation> stockValuation() {
        String sql = """
                SELECT c.id AS category_id,
                       COALESCE(c.name, 'Uncategorized') AS category_name,
                       SUM(p.quantity) AS total_units,
                       SUM(p.price * p.quantity) AS total_value
                FROM product p
                LEFT JOIN category c ON c.id = p.category_id
                GROUP BY c.id, c.name
                HAVING SUM(p.quantity) > 0
                ORDER BY total_value DESC, category_name
                """;
        return jdbc.query(sql, (rs, i) -> new CategoryStockValuation(
                rs.getObject("category_id", Long.class), rs.getString("category_name"),
                rs.getLong("total_units"), rs.getBigDecimal("total_value")));
    }

    @Override
    public List<LowStockItem> lowStock() {
        String sql = """
                SELECT id, sku, name, quantity, minimum_quantity, minimum_quantity - quantity AS shortfall
                FROM product
                WHERE active AND quantity <= minimum_quantity
                ORDER BY shortfall DESC, name
                """;
        return jdbc.query(sql, (rs, i) -> new LowStockItem(rs.getLong("id"), rs.getString("sku"),
                rs.getString("name"), rs.getInt("quantity"), rs.getInt("minimum_quantity"), rs.getInt("shortfall")));
    }

    private MapSqlParameterSource range(Instant from, Instant to) {
        return new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
    }
}
