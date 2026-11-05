package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.application.mapper.ProductMapper;
import com.stockpilot.core.domain.Category;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.AuditEntry;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.port.out.AuditLogRepository;
import com.stockpilot.core.port.out.CurrentUserProvider;
import com.stockpilot.application.support.ProductFixtures;
import org.mockito.ArgumentCaptor;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.ProductUpdateRequest;
import com.stockpilot.core.port.out.CategoryRepository;
import com.stockpilot.core.port.out.DomainEventPublisher;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.StockMovementRepository;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.StockMovement;
import com.stockpilot.core.port.in.dto.RestockRequest;
import org.mockito.ArgumentCaptor;
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
    @Mock private StockMovementRepository movementRepository;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private AuditLogRepository auditLog;
    @Mock private CurrentUserProvider currentUser;

    private ProductService service;
    private Product coffee;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, categoryRepository,
                new StockLedger(movementRepository, eventPublisher), new ProductMapper(), auditLog, currentUser);
        coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 10, 3);
        ProductFixtures.setId(coffee, 1L);
        lenient().when(currentUser.currentUserOrSystem()).thenReturn("alice@shop.com");
    }

    @Test
    void shouldRegisterProductWithNormalizedSkuAndCategory() {
        Category drinks = Category.create("Drinks", null);
        when(productRepository.existsBySku("TEA-1")).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(drinks));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

    @Test
    void shouldRecordOpeningBalanceWhenRegisteringProductWithStock() {
        when(productRepository.existsBySku("TEA-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(new ProductRequest("TEA-1", "Tea", null, BigDecimal.ONE, 7, 0, null));

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(MovementType.IN);
        assertThat(captor.getValue().getQuantity()).isEqualTo(7);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(7);
    }

    @Test
    void shouldNotRecordMovementWhenRegisteringProductWithoutStock() {
        when(productRepository.existsBySku("TEA-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(new ProductRequest("TEA-1", "Tea", null, BigDecimal.ONE, 0, 0, null));

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRecordInMovementOnRestock() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(coffee));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.restock(1L, new RestockRequest(5));

        assertThat(response.quantity()).isEqualTo(15);
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(MovementType.IN);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(15);
    }

    @Test
    void shouldFailToRestockUnknownProduct() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restock(1L, new RestockRequest(5)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectNonPositiveRestockWithoutRecordingMovement() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(coffee));

        assertThatThrownBy(() -> service.restock(1L, new RestockRequest(0)))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldAuditAPriceChangeWithOldAndNewValueAndUser() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        service.update(1L, new ProductUpdateRequest("Coffee", null, new BigDecimal("25.5"), 3, null));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLog).save(captor.capture());
        AuditEntry entry = captor.getValue();
        assertThat(entry.getEntityType()).isEqualTo("Product");
        assertThat(entry.getEntityId()).isEqualTo(1L);
        assertThat(entry.getField()).isEqualTo("price");
        assertThat(entry.getOldValue()).isEqualTo("20.00");
        assertThat(entry.getNewValue()).isEqualTo("25.50");
        assertThat(entry.getChangedBy()).isEqualTo("alice@shop.com");
    }

    @Test
    void shouldNotAuditWhenOnlyTheNameChanges() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        service.update(1L, new ProductUpdateRequest("Coffee XL", null, new BigDecimal("20.00"), 3, null));

        verify(auditLog, never()).save(any());
    }

    @Test
    void shouldNotAuditAPriceThatOnlyDiffersInScale() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        service.update(1L, new ProductUpdateRequest("Coffee", null, new BigDecimal("20.0"), 3, null));

        verify(auditLog, never()).save(any());
    }

    @Test
    void shouldNotAuditWhenTheUpdateIsRejected() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        assertThatThrownBy(() -> service.update(1L, new ProductUpdateRequest("Coffee", null, new BigDecimal("-1"), 3, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(auditLog, never()).save(any());
    }
}
