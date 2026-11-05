package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.application.mapper.SaleMapper;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.event.LowStockEvent;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.SaleRepository;
import com.stockpilot.core.port.out.StockMovementRepository;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.StockMovement;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.stockpilot.core.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private StockMovementRepository movementRepository;

    private SaleService saleService;
    private Product coffee;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(productRepository, saleRepository, new SaleMapper(),
                new StockLedger(movementRepository, eventPublisher));
        coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 10, 3);
    }

    private SaleRequest requestFor(int quantity, BigDecimal discount) {
        return new SaleRequest(PaymentMethod.PIX, discount, List.of(new SaleItemRequest(1L, quantity)));
    }

    @Test
    void shouldCheckoutAndDecreaseStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = saleService.checkout(requestFor(2, null));

        assertThat(response.total()).isEqualByComparingTo("40.00");
        assertThat(coffee.getQuantity()).isEqualTo(8);
        verify(eventPublisher, never()).publish(any(LowStockEvent.class));
    }

    @Test
    void shouldRecordOutMovementWithSaleReference() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        saleService.checkout(requestFor(4, null));

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        StockMovement movement = captor.getValue();
        assertThat(movement.getType()).isEqualTo(MovementType.OUT);
        assertThat(movement.getQuantity()).isEqualTo(-4);
        assertThat(movement.getBalanceAfter()).isEqualTo(6);
        assertThat(movement.getReference()).startsWith("SALE-");
    }

    @Test
    void shouldApplyDiscount() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = saleService.checkout(requestFor(5, new BigDecimal("10")));

        assertThat(response.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(response.total()).isEqualByComparingTo("90.00");
    }

    @Test
    void shouldPublishLowStockEventWhenStockReachesMinimum() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        saleService.checkout(requestFor(7, null));

        verify(eventPublisher).publish(any(LowStockEvent.class));
    }

    @Test
    void shouldFailWhenStockIsInsufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(requestFor(11, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveProduct() {
        coffee.deactivate();
        when(productRepository.findById(1L)).thenReturn(Optional.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(requestFor(1, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");
        assertThat(coffee.getQuantity()).isEqualTo(10);
        verify(saleRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenProductDoesNotExist() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.checkout(requestFor(1, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
