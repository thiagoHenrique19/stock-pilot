package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.domain.SaleStatus;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.ReceiptNumberGenerator;
import com.stockpilot.core.port.out.SaleRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SaleFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SalePersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private SaleRepository saleRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ReceiptNumberGenerator receiptNumberGenerator;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Product product;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sale_item");
        jdbcTemplate.execute("DELETE FROM sale");
        product = productRepository.save(Product.create(
                "SALE-" + UUID.randomUUID().toString().substring(0, 8), "Tea", new BigDecimal("10.00"), 100, 1));
    }

    private Sale save(PaymentMethod method, boolean cancelled) {
        Sale sale = Sale.open(method, receiptNumberGenerator.next());
        sale.addItem(product, 1);
        if (cancelled) {
            sale.cancel();
        }
        return saleRepository.save(sale);
    }

    @Test
    void shouldGenerateIncreasingFormattedReceiptNumbers() {
        String first = receiptNumberGenerator.next();
        String second = receiptNumberGenerator.next();

        assertThat(first).matches("SALE-\\d{6}");
        assertThat(Long.parseLong(second.substring(5))).isEqualTo(Long.parseLong(first.substring(5)) + 1);
    }

    @Test
    void shouldPersistStatusCashAmountsAndFindById() {
        Sale sale = Sale.open(PaymentMethod.CASH, receiptNumberGenerator.next());
        sale.addItem(product, 2);
        sale.receivePayment(new BigDecimal("50.00"));
        Sale saved = saleRepository.save(sale);

        Sale found = saleRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(SaleStatus.COMPLETED);
        assertThat(found.getChange()).isEqualByComparingTo("30.00");
        assertThat(found.getReceiptNumber()).isEqualTo(sale.getReceiptNumber());
    }

    @Test
    void shouldFilterByPaymentMethodAndStatus() {
        save(PaymentMethod.PIX, false);
        save(PaymentMethod.PIX, true);
        save(PaymentMethod.CASH, false);

        assertThat(saleRepository.search(new SaleFilter(null, null, PaymentMethod.PIX, null),
                PageQuery.firstPage()).totalElements()).isEqualTo(2);
        assertThat(saleRepository.search(new SaleFilter(null, null, null, SaleStatus.CANCELLED),
                PageQuery.firstPage()).totalElements()).isEqualTo(1);
        assertThat(saleRepository.search(new SaleFilter(null, null, PaymentMethod.CASH, SaleStatus.COMPLETED),
                PageQuery.firstPage()).totalElements()).isEqualTo(1);
    }

    @Test
    void shouldFilterByDateRangeWithInclusiveStartAndExclusiveEnd() {
        save(PaymentMethod.PIX, false);
        Instant now = Instant.now();

        assertThat(saleRepository.search(new SaleFilter(now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                null, null), PageQuery.firstPage()).totalElements()).isEqualTo(1);
        assertThat(saleRepository.search(new SaleFilter(now.plus(1, ChronoUnit.HOURS), null, null, null),
                PageQuery.firstPage()).totalElements()).isZero();
        assertThat(saleRepository.search(new SaleFilter(null, now.minus(1, ChronoUnit.HOURS), null, null),
                PageQuery.firstPage()).totalElements()).isZero();
    }

    @Test
    void shouldPaginateNewestFirstByDefault() {
        Sale first = save(PaymentMethod.PIX, false);
        Sale second = save(PaymentMethod.PIX, false);
        Sale third = save(PaymentMethod.PIX, false);

        PageResult<Sale> page = saleRepository.search(new SaleFilter(null, null, null, null), PageQuery.of(0, 2, null, null));

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content()).extracting(Sale::getId).containsExactly(third.getId(), second.getId());
        assertThat(saleRepository.search(new SaleFilter(null, null, null, null), PageQuery.of(1, 2, null, null))
                .content()).extracting(Sale::getId).containsExactly(first.getId());
    }
}
