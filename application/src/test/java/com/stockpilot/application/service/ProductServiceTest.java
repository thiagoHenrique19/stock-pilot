package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.application.mapper.ProductMapper;
import com.stockpilot.core.domain.Category;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.ProductUpdateRequest;
import com.stockpilot.core.port.out.CategoryRepository;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    private ProductService service;
    private Product coffee;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, categoryRepository, new ProductMapper());
        coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 10, 3);
    }

    @Test
    void shouldRegisterProductWithNormalizedSkuAndCategory() {
        Category drinks = Category.create("Drinks", null);
        when(productRepository.existsBySku("TEA-1")).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(drinks));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.register(
                new ProductRequest(" tea-1 ", "Tea", "Green", new BigDecimal("5.00"), 10, 2, 2L));

        assertThat(response.sku()).isEqualTo("TEA-1");
        assertThat(response.categoryName()).isEqualTo("Drinks");
        assertThat(response.active()).isTrue();
    }

    @Test
    void shouldRejectDuplicatedSkuIgnoringCase() {
        when(productRepository.existsBySku("TEA-1")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new ProductRequest("tea-1", "Tea", null, BigDecimal.ONE, 1, 0, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenCategoryDoesNotExist() {
        when(productRepository.existsBySku("TEA-1")).thenReturn(false);
        when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                new ProductRequest("TEA-1", "Tea", null, BigDecimal.ONE, 1, 0, 5L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        ProductResponse response = service.update(1L,
                new ProductUpdateRequest("Coffee XL", "Big", new BigDecimal("30.00"), 5, null));

        assertThat(response.name()).isEqualTo("Coffee XL");
        assertThat(response.price()).isEqualByComparingTo("30.00");
        assertThat(response.minimumQuantity()).isEqualTo(5);
    }

    @Test
    void shouldDeactivateAndReactivateProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        assertThat(service.deactivate(1L).active()).isFalse();
        assertThat(service.reactivate(1L).active()).isTrue();
    }

    @Test
    void shouldFailToDeactivateUnknownProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldSearchProductsDelegatingToRepository() {
        ProductSearchCriteria criteria = new ProductSearchCriteria("cof", null, true, false);
        PageQuery pageQuery = PageQuery.firstPage();
        when(productRepository.search(criteria, pageQuery))
                .thenReturn(new PageResult<>(List.of(coffee), 0, 20, 1));

        PageResult<ProductResponse> result = service.search(criteria, pageQuery);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).extracting(ProductResponse::name).containsExactly("Coffee");
    }
}
