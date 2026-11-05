package com.stockpilot.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.concurrency.ConcurrentRun.Outcome;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConcurrentCheckoutIntegrationTest extends ConcurrencyTestSupport {

    @Autowired private CheckoutSaleUseCase checkout;

    private SaleRequest sale(long productId, int quantity) {
        return new SaleRequest(PaymentMethod.PIX, null, null, null, List.of(new SaleItemRequest(productId, quantity)));
    }

    @Test
    void shouldSellExactlyTheAvailableUnitsWhenManyBuyersRaceForTheLastOnes() throws Exception {
        long productId = newProduct(5);
        List<Callable<?>> buyers = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            buyers.add(() -> checkout.checkout(sale(productId, 1)));
        }

        List<Outcome> outcomes = ConcurrentRun.all(buyers);

        long sold = outcomes.stream().filter(Outcome::succeeded).count();
        assertThat(sold).isEqualTo(5);
        assertThat(outcomes.stream().filter(o -> !o.succeeded()))
                .hasSize(15)
                .allSatisfy(o -> assertThat(o.error()).isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("Insufficient stock"));
        assertThat(quantityOf(productId)).isZero();
        assertThat(ledgerSumOf(productId)).isZero();
        assertThat(balanceChainBreaks(productId)).isEmpty();
    }

    @Test
    void shouldNeverSellMoreThanStockWhenBuyersAskForDifferentQuantities() throws Exception {
        long productId = newProduct(10);
        List<Callable<?>> buyers = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int quantity = 1 + i % 3;
            buyers.add(() -> checkout.checkout(sale(productId, quantity)));
        }

        List<Outcome> outcomes = ConcurrentRun.all(buyers);

        int soldUnits = jdbc.queryForObject("SELECT COALESCE(SUM(quantity), 0) FROM sale_item WHERE product_id = ?",
                Integer.class, productId);
        assertThat(outcomes.stream().filter(o -> !o.succeeded()))
                .allSatisfy(o -> assertThat(o.error()).isInstanceOf(BusinessRuleException.class));
        assertThat(soldUnits).isLessThanOrEqualTo(10);
        assertThat(quantityOf(productId)).isEqualTo(10 - soldUnits).isGreaterThanOrEqualTo(0);
        assertThat(balanceChainBreaks(productId)).isEmpty();
    }
}
