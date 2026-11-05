package com.stockpilot.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.concurrency.ConcurrentRun.Outcome;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.exception.RequestInProgressException;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase.Result;
import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConcurrentIdempotencyIntegrationTest extends ConcurrencyTestSupport {

    @Autowired private CheckoutSaleUseCase checkout;
    @Autowired private ExecuteIdempotentlyUseCase idempotency;

    @Test
    void shouldCreateExactlyOneSaleWhenTheSameRequestArrivesTwentyTimesAtOnce() throws Exception {
        long productId = newProduct(100);
        String key = UUID.randomUUID().toString();
        SaleRequest request = new SaleRequest(PaymentMethod.PIX, null, null, null,
                List.of(new SaleItemRequest(productId, 2)));
        List<Callable<?>> calls = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            calls.add(() -> idempotency.execute("sales.checkout", key, request, SaleResponse.class,
                    () -> checkout.checkout(request)));
        }

        List<Outcome> outcomes = ConcurrentRun.all(calls);

        assertThat(outcomes.stream().filter(Outcome::succeeded)).isNotEmpty();
        assertThat(outcomes.stream().filter(o -> !o.succeeded()))
                .allSatisfy(o -> assertThat(o.error()).isInstanceOf(RequestInProgressException.class));
        List<Outcome> created = outcomes.stream().filter(Outcome::succeeded)
                .filter(o -> !((Result<?>) o.value()).replayed()).toList();
        assertThat(created).hasSize(1);
        long saleId = ((SaleResponse) ((Result<?>) created.get(0).value()).body()).id();
        assertThat(outcomes.stream().filter(Outcome::succeeded))
                .allSatisfy(o -> assertThat(((SaleResponse) ((Result<?>) o.value()).body()).id()).isEqualTo(saleId));
        assertThat(quantityOf(productId)).isEqualTo(98);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sale_item WHERE product_id = ?", Integer.class, productId))
                .isEqualTo(1);
    }

    @Test
    void shouldReplayTheSameSaleOnceTheFirstRequestHasFinished() throws Exception {
        long productId = newProduct(10);
        String key = UUID.randomUUID().toString();
        SaleRequest request = new SaleRequest(PaymentMethod.PIX, null, null, null,
                List.of(new SaleItemRequest(productId, 1)));
        Result<SaleResponse> first = idempotency.execute("sales.checkout", key, request, SaleResponse.class,
                () -> checkout.checkout(request));

        List<Callable<?>> retries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            retries.add(() -> idempotency.execute("sales.checkout", key, request, SaleResponse.class,
                    () -> checkout.checkout(request)));
        }
        List<Outcome> outcomes = ConcurrentRun.all(retries);

        assertThat(outcomes).allSatisfy(o -> {
            assertThat(o.error()).isNull();
            Result<?> result = (Result<?>) o.value();
            assertThat(result.replayed()).isTrue();
            assertThat(((SaleResponse) result.body()).id()).isEqualTo(first.body().id());
        });
        assertThat(quantityOf(productId)).isEqualTo(9);
    }

    @Test
    void shouldNotLeaveTheKeyBehindWhenAConcurrentFailureHappens() throws Exception {
        long productId = newProduct(1);
        List<Callable<?>> calls = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String key = UUID.randomUUID().toString();
            SaleRequest request = new SaleRequest(PaymentMethod.PIX, null, null, null,
                    List.of(new SaleItemRequest(productId, 1)));
            calls.add(() -> idempotency.execute("sales.checkout", key, request, SaleResponse.class,
                    () -> checkout.checkout(request)));
        }

        List<Outcome> outcomes = ConcurrentRun.all(calls);

        assertThat(outcomes.stream().filter(Outcome::succeeded)).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_key WHERE status = 'IN_PROGRESS'",
                Integer.class)).isZero();
    }
}
