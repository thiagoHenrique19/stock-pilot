package com.stockpilot.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.concurrency.ConcurrentRun.Outcome;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.port.in.AdjustStockUseCase;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.dto.RestockRequest;
import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.StockAdjustmentRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConcurrentRestockIntegrationTest extends ConcurrencyTestSupport {

    @Autowired private CheckoutSaleUseCase checkout;
    @Autowired private AdjustStockUseCase adjustments;

    @Test
    void shouldKeepLedgerAndStockConsistentWhenRestocksAndSalesInterleave() throws Exception {
        long productId = newProduct(50);
        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            tasks.add(() -> checkout.checkout(new SaleRequest(PaymentMethod.PIX, null, null, null,
                    List.of(new SaleItemRequest(productId, 1)))));
            tasks.add(() -> products.restock(productId, new RestockRequest(2)));
        }

        List<Outcome> outcomes = ConcurrentRun.all(tasks);

        assertThat(outcomes).allSatisfy(o -> assertThat(o.error()).isNull());
        assertThat(quantityOf(productId)).isEqualTo(50 - 25 + 50);
        assertThat(ledgerSumOf(productId)).isEqualTo(75);
        assertThat(balanceChainBreaks(productId)).isEmpty();
    }

    @Test
    void shouldNotLoseAdjustmentsMadeAtTheSameTime() throws Exception {
        long productId = newProduct(100);
        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            int delta = i % 2 == 0 ? 3 : -2;
            tasks.add(() -> adjustments.adjust(productId, new StockAdjustmentRequest(delta, "Count")));
        }

        List<Outcome> outcomes = ConcurrentRun.all(tasks);

        assertThat(outcomes).allSatisfy(o -> assertThat(o.error()).isNull());
        assertThat(quantityOf(productId)).isEqualTo(100 + 15 * 3 - 15 * 2);
        assertThat(ledgerSumOf(productId)).isEqualTo(115);
        assertThat(balanceChainBreaks(productId)).isEmpty();
    }
}
