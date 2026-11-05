package com.stockpilot.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.out.ProductRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.MediaType;

@TestPropertySource(properties = "app.locking.timeout=700ms")
class LockTimeoutIntegrationTest extends ConcurrencyTestSupport {

    @Autowired private CheckoutSaleUseCase checkout;
    @Autowired private ProductRepository productRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldGiveUpWaitingForALockedProductInsteadOfBlockingForever() throws Exception {
        long productId = newProduct(5);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> transactionTemplate.executeWithoutResult(status -> {
            productRepository.findAllByIdForUpdate(List.of(productId));
            locked.countDown();
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        holder.start();
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        long started = System.nanoTime();
        try {
            assertThatThrownBy(() -> checkout.checkout(new SaleRequest(PaymentMethod.PIX, null, null, null,
                    List.of(new SaleItemRequest(productId, 1)))))
                    .isInstanceOf(PessimisticLockingFailureException.class);
        } finally {
            release.countDown();
            holder.join();
        }

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(600L, 5_000L);
        assertThat(quantityOf(productId)).isEqualTo(5);
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void shouldAnswerConflictOverHttpWhenTheProductStaysLocked() throws Exception {
        long productId = newProduct(5);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> transactionTemplate.executeWithoutResult(status -> {
            productRepository.findAllByIdForUpdate(List.of(productId));
            locked.countDown();
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        holder.start();
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        try {
            mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"paymentMethod\":\"PIX\",\"items\":[{\"productId\":%d,\"quantity\":1}]}"
                                    .formatted(productId)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:stockpilot:problem:concurrent-update"));
        } finally {
            release.countDown();
            holder.join();
        }
    }
}
