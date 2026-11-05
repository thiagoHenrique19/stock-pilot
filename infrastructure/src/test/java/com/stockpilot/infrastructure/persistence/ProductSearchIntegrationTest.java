package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.Category;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.port.out.CategoryRepository;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProductSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;

    private String tag;
    private Category drinks;

    @BeforeEach
    void seed() {
        tag = "t" + UUID.randomUUID().toString().substring(0, 8);
        drinks = categoryRepository.save(Category.create("Drinks " + tag, null));
        Category food = categoryRepository.save(Category.create("Food " + tag, null));
        save("A", tag + " Coffee Beans", "30.00", 50, 5, drinks);
        save("B", tag + " cola", "5.00", 2, 5, drinks);
        save("C", tag + " Rice", "9.00", 20, 5, food);
        Product old = save("D", tag + " Coffee Mug Old", "12.00", 3, 1, food);
        old.deactivate();
    }

    private Product save(String suffix, String name, String price, int quantity, int minimum, Category category) {
        return productRepository.save(Product.create(tag + "-" + suffix, name, null, new BigDecimal(price),
                quantity, minimum, category));
    }

    private ProductSearchCriteria criteria(Long categoryId, Boolean active, boolean lowStock, String name) {
        return new ProductSearchCriteria(name, categoryId, active, lowStock);
    }

    @Test
    void shouldFilterByNameIgnoringCase() {
        PageResult<Product> result = productRepository.search(
                criteria(null, null, false, tag.toUpperCase() + " COFFEE"), PageQuery.firstPage());

        assertThat(result.content()).extracting(Product::getSku).containsExactlyInAnyOrder(tag.toUpperCase() + "-A", tag.toUpperCase() + "-D");
    }

    @Test
    void shouldFilterByCategoryAndActiveFlag() {
        PageResult<Product> result = productRepository.search(
                criteria(drinks.getId(), true, false, tag), PageQuery.firstPage());

        assertThat(result.content()).hasSize(2);
        assertThat(productRepository.search(criteria(null, false, false, tag), PageQuery.firstPage()).content())
                .extracting(Product::getName).containsExactly(tag + " Coffee Mug Old");
    }

    @Test
    void shouldFilterLowStockOnly() {
        PageResult<Product> result = productRepository.search(
                criteria(null, true, true, tag), PageQuery.firstPage());

        assertThat(result.content()).extracting(Product::getName).containsExactly(tag + " cola");
    }

    @Test
    void shouldPaginateAndSort() {
        PageResult<Product> first = productRepository.search(
                criteria(null, null, false, tag), PageQuery.of(0, 3, "price", "desc"));
        PageResult<Product> second = productRepository.search(
                criteria(null, null, false, tag), PageQuery.of(1, 3, "price", "desc"));

        assertThat(first.totalElements()).isEqualTo(4);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.content()).extracting(Product::getPrice)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("30.00"), new BigDecimal("12.00"), new BigDecimal("9.00"));
        assertThat(second.content()).extracting(Product::getPrice).hasSize(1);
    }

    @Test
    void shouldRejectUnknownSortField() {
        assertThatThrownBy(() -> productRepository.search(
                ProductSearchCriteria.all(), PageQuery.of(0, 10, "password; drop", null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldFindCategoryNameIgnoringCase() {
        assertThat(categoryRepository.existsByNameIgnoreCase(("drinks " + tag).toUpperCase())).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("missing " + tag)).isFalse();
    }
}
