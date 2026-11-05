package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.application.mapper.ProductMapper;
import com.stockpilot.application.mapper.StockMovementMapper;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.StockMovement;
import com.stockpilot.core.event.LowStockEvent;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.StockAdjustmentRequest;
import com.stockpilot.core.port.in.dto.StockMovementResponse;
import com.stockpilot.core.port.out.DomainEventPublisher;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.StockMovementRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StockMovementRepository movementRepository;
    @Mock private DomainEventPublisher eventPublisher;

    private StockMovementService service;
    private Product coffee;

    @BeforeEach
    void setUp() {
        service = new StockMovementService(productRepository, movementRepository,
                new StockLedger(movementRepository, eventPublisher), new ProductMapper(), new StockMovementMapper());
        coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 10, 3);
    }

    @Test
    void shouldAdjustStockAndRecordMovementWithReason() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.adjust(1L, new StockAdjustmentRequest(-2, "Damaged"));

        assertThat(response.quantity()).isEqualTo(8);
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(captor.getValue().getQuantity()).isEqualTo(-2);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(8);
        assertThat(captor.getValue().getReason()).isEqualTo("Damaged");
    }

    @Test
    void shouldPublishLowStockEventWhenNegativeAdjustmentReachesMinimum() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.adjust(1L, new StockAdjustmentRequest(-7, "Inventory count"));

        verify(eventPublisher).publish(any(LowStockEvent.class));
    }

    @Test
    void shouldNotPublishLowStockEventForPositiveAdjustment() {
        coffee.removeStock(8);
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.adjust(1L, new StockAdjustmentRequest(1, "Found in storage"));

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectAdjustmentBelowZeroWithoutRecordingMovement() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        assertThatThrownBy(() -> service.adjust(1L, new StockAdjustmentRequest(-11, "Loss")))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenProductDoesNotExist() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjust(1L, new StockAdjustmentRequest(1, "x")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.movementsOf(1L, PageQuery.firstPage()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldListMovementsOfProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));
        StockMovement movement = StockMovement.record(coffee, MovementType.IN, 5, "Restock", null);
        PageQuery pageQuery = PageQuery.firstPage();
        when(movementRepository.findByProductId(1L, pageQuery)).thenReturn(new PageResult<>(List.of(movement), 0, 20, 1));

        PageResult<StockMovementResponse> result = service.movementsOf(1L, pageQuery);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).type()).isEqualTo(MovementType.IN);
    }
}
