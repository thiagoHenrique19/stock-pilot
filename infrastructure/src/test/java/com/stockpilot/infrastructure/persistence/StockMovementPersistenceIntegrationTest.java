package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.StockMovement;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.StockMovementRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class StockMovementPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private StockMovementRepository movementRepository;

    @Test
    void shouldPersistMovementsAndReturnNewestFirstPaginated() {
        Product product = productRepository.save(Product.create(
                "MOV-" + UUID.randomUUID().toString().substring(0, 8), "Tea", new BigDecimal("3.00"), 10, 2));
        for (int i = 1; i <= 5; i++) {
            product.addStock(1);
            movementRepository.save(StockMovement.record(product, MovementType.IN, 1, "Restock " + i, "REF-" + i));
        }

        PageResult<StockMovement> first = movementRepository.findByProductId(product.getId(), PageQuery.of(0, 2, null, null));
        PageResult<StockMovement> last = movementRepository.findByProductId(product.getId(), PageQuery.of(2, 2, null, null));

        assertThat(first.totalElements()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.content()).extracting(StockMovement::getReason).containsExactly("Restock 5", "Restock 4");
        assertThat(first.content().get(0).getBalanceAfter()).isEqualTo(15);
        assertThat(last.content()).extracting(StockMovement::getReason).containsExactly("Restock 1");
    }

    @Test
    void shouldNotMixMovementsOfDifferentProducts() {
        Product first = productRepository.save(Product.create(
                "A-" + UUID.randomUUID().toString().substring(0, 8), "A", BigDecimal.ONE, 1, 0));
        Product second = productRepository.save(Product.create(
                "B-" + UUID.randomUUID().toString().substring(0, 8), "B", BigDecimal.ONE, 1, 0));
        movementRepository.save(StockMovement.record(first, MovementType.IN, 1, "x", null));

        assertThat(movementRepository.findByProductId(second.getId(), PageQuery.firstPage()).content()).isEmpty();
    }
}
