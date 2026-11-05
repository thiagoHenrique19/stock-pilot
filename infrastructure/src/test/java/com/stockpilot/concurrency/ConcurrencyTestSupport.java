package com.stockpilot.concurrency;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import com.stockpilot.core.port.in.dto.ProductRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

abstract class ConcurrencyTestSupport extends AbstractIntegrationTest {

    @Autowired protected ManageProductsUseCase products;
    @Autowired protected JdbcTemplate jdbc;

    protected long newProduct(int quantity) {
        String sku = "CC-" + UUID.randomUUID().toString().substring(0, 8);
        return products.register(new ProductRequest(sku, "Concurrent " + sku, null,
                new BigDecimal("10.00"), quantity, 0, null)).id();
    }

    protected int quantityOf(long productId) {
        return jdbc.queryForObject("SELECT quantity FROM product WHERE id = ?", Integer.class, productId);
    }

    protected int ledgerSumOf(long productId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(quantity), 0) FROM stock_movement WHERE product_id = ?",
                Integer.class, productId);
    }

    /** Every movement must carry the balance that results from the one before it. */
    protected List<Integer> balanceChainBreaks(long productId) {
        List<int[]> rows = jdbc.query(
                "SELECT quantity, balance_after FROM stock_movement WHERE product_id = ? ORDER BY id",
                (rs, i) -> new int[]{rs.getInt(1), rs.getInt(2)}, productId);
        List<Integer> breaks = new java.util.ArrayList<>();
        int running = 0;
        for (int i = 0; i < rows.size(); i++) {
            running += rows.get(i)[0];
            if (running != rows.get(i)[1]) {
                breaks.add(i);
            }
        }
        return breaks;
    }
}
