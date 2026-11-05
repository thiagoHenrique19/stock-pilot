package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.port.out.ProductRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProductLockingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private Product newProduct() {
        return productRepository.save(Product.create(
                "FU-" + UUID.randomUUID().toString().substring(0, 8), "Lock", new BigDecimal("1.00"), 5, 0));
    }

    @Test
    void shouldReturnLockedProductsOrderedByIdWhateverTheRequestOrder() {
        Product a = newProduct();
        Product b = newProduct();
        Product c = newProduct();

        List<Product> locked = productRepository.findAllByIdForUpdate(List.of(c.getId(), a.getId(), b.getId()));

        assertThat(locked).extracting(Product::getId).containsExactly(a.getId(), b.getId(), c.getId());
    }

    @Test
    void shouldSkipUnknownIdsAndCollapseDuplicates() {
        Product a = newProduct();

        List<Product> locked = productRepository.findAllByIdForUpdate(List.of(a.getId(), a.getId(), -1L));

        assertThat(locked).extracting(Product::getId).containsExactly(a.getId());
    }

    @Test
    void shouldReturnEmptyListForNoIds() {
        assertThat(productRepository.findAllByIdForUpdate(List.of())).isEmpty();
    }

    @Test
    void shouldReturnCurrentRowStateEvenWhenTheProductWasAlreadyLoaded() {
        Product product = newProduct();
        entityManager.flush();
        jdbcTemplate.update("UPDATE product SET quantity = 42 WHERE id = ?", product.getId());

        List<Product> locked = productRepository.findAllByIdForUpdate(List.of(product.getId()));

        assertThat(locked.get(0).getQuantity()).isEqualTo(42);
    }
}
