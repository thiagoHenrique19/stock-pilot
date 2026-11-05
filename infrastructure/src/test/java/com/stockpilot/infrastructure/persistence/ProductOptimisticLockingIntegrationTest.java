package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.port.out.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

class ProductOptimisticLockingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Product newProduct(int quantity) {
        return productRepository.save(Product.create(
                "LCK-" + UUID.randomUUID().toString().substring(0, 8), "Lock test", new BigDecimal("5.00"), quantity, 0));
    }

    @Test
    void shouldRejectUpdateBasedOnStaleVersion() {
        Long id = newProduct(10).getId();
        Product first = transactionTemplate.execute(status -> productRepository.findById(id).orElseThrow());
        Product second = transactionTemplate.execute(status -> productRepository.findById(id).orElseThrow());

        first.removeStock(3);
        productRepository.save(first);
        second.removeStock(4);

        assertThatThrownBy(() -> productRepository.save(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(productRepository.findById(id).orElseThrow().getQuantity()).isEqualTo(7);
    }

    @Test
    void shouldIncrementVersionOnEveryChange() {
        Product product = newProduct(10);
        long initial = product.getVersion();

        product.addStock(1);
        Product updated = productRepository.save(product);

        assertThat(updated.getVersion()).isEqualTo(initial + 1);
    }
}
