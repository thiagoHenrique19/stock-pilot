package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.application.mapper.SaleMapper;
import com.stockpilot.application.support.ProductFixtures;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.event.LowStockEvent;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.ReceiptNumberGenerator;
import com.stockpilot.core.port.out.SaleRepository;
import com.stockpilot.core.port.out.StockMovementRepository;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.StockMovement;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.List;
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
    @Mock private ReceiptNumberGenerator receiptNumberGenerator;

    private SaleService saleService;
    private Product coffee;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(productRepository, saleRepository, new SaleMapper(),
                new StockLedger(movementRepository, eventPublisher), receiptNumberGenerator);
        lenient().when(receiptNumberGenerator.next()).thenReturn("SALE-000123");
        coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 10, 3);
        ProductFixtures.setId(coffee, 1L);
    }

    private SaleRequest requestFor(int quantity, BigDecimal discount) {
        return new SaleRequest(PaymentMethod.PIX, discount, null, null, List.of(new SaleItemRequest(1L, quantity)));
    }

    private void stubSave() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCheckoutAndDecreaseStock() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = saleService.checkout(requestFor(2, null));

        assertThat(response.total()).isEqualByComparingTo("40.00");
        assertThat(coffee.getQuantity()).isEqualTo(8);
        verify(eventPublisher, never()).publish(any(LowStockEvent.class));
    }

    @Test
    void shouldRecordOutMovementWithSaleReference() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        saleService.checkout(requestFor(4, null));

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        StockMovement movement = captor.getValue();
        assertThat(movement.getType()).isEqualTo(MovementType.OUT);
        assertThat(movement.getQuantity()).isEqualTo(-4);
        assertThat(movement.getBalanceAfter()).isEqualTo(6);
        assertThat(movement.getReference()).isEqualTo("SALE-000123");
    }

    @Test
    void shouldApplyDiscount() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = saleService.checkout(requestFor(5, new BigDecimal("10")));

        assertThat(response.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(response.total()).isEqualByComparingTo("90.00");
    }

    @Test
    void shouldPublishLowStockEventWhenStockReachesMinimum() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        saleService.checkout(requestFor(7, null));

        verify(eventPublisher).publish(any(LowStockEvent.class));
    }

    @Test
    void shouldFailWhenStockIsInsufficient() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(requestFor(11, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveProduct() {
        coffee.deactivate();
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(requestFor(1, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");
        assertThat(coffee.getQuantity()).isEqualTo(10);
        verify(saleRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenProductDoesNotExist() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of());

        assertThatThrownBy(() -> saleService.checkout(requestFor(1, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldAssignReceiptNumberAndStatus() {
        stubSave();

        SaleResponse response = saleService.checkout(requestFor(1, null));

        assertThat(response.receiptNumber()).isEqualTo("SALE-000123");
        assertThat(response.status()).isEqualTo(com.stockpilot.core.domain.SaleStatus.COMPLETED);
    }

    @Test
    void shouldApplyFixedAmountDiscount() {
        stubSave();

        SaleResponse response = saleService.checkout(new SaleRequest(PaymentMethod.PIX, null,
                new BigDecimal("15.00"), null, List.of(new SaleItemRequest(1L, 2))));

        assertThat(response.discountAmount()).isEqualByComparingTo("15.00");
        assertThat(response.total()).isEqualByComparingTo("25.00");
    }

    @Test
    void shouldRejectPercentageAndAmountTogether() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(new SaleRequest(PaymentMethod.PIX, new BigDecimal("10"),
                new BigDecimal("5"), null, List.of(new SaleItemRequest(1L, 1)))))
                .isInstanceOf(BusinessRuleException.class);
        verify(saleRepository, never()).save(any());
    }

    @Test
    void shouldRejectDiscountAmountAboveSubtotal() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(new SaleRequest(PaymentMethod.PIX, null,
                new BigDecimal("21.00"), null, List.of(new SaleItemRequest(1L, 1)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldReturnChangeForCashPayment() {
        stubSave();

        SaleResponse response = saleService.checkout(new SaleRequest(PaymentMethod.CASH, null, null,
                new BigDecimal("50.00"), List.of(new SaleItemRequest(1L, 2))));

        assertThat(response.amountReceived()).isEqualByComparingTo("50.00");
        assertThat(response.change()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldRejectCashAmountThatDoesNotCoverTotal() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(new SaleRequest(PaymentMethod.CASH, null, null,
                new BigDecimal("10.00"), List.of(new SaleItemRequest(1L, 2)))))
                .isInstanceOf(BusinessRuleException.class);
        verify(saleRepository, never()).save(any());
    }

    @Test
    void shouldLockProductsInAscendingIdOrderWhateverTheRequestOrder() {
        Product tea = ProductFixtures.product(2L, "T-1", 5);
        when(productRepository.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(coffee, tea));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = saleService.checkout(new SaleRequest(PaymentMethod.PIX, null, null, null,
                List.of(new SaleItemRequest(2L, 1), new SaleItemRequest(1L, 1))));

        assertThat(response.items()).extracting(item -> item.productId()).containsExactly(2L, 1L);
        assertThat(coffee.getQuantity()).isEqualTo(9);
        assertThat(tea.getQuantity()).isEqualTo(4);
    }

    @Test
    void shouldLockEachProductOnceWhenTheSameProductAppearsTwice() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        saleService.checkout(new SaleRequest(PaymentMethod.PIX, null, null, null,
                List.of(new SaleItemRequest(1L, 4), new SaleItemRequest(1L, 5))));

        assertThat(coffee.getQuantity()).isEqualTo(1);
    }

    @Test
    void shouldNotAllowTwoLinesOfTheSameProductToExceedStock() {
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(new SaleRequest(PaymentMethod.PIX, null, null, null,
                List.of(new SaleItemRequest(1L, 6), new SaleItemRequest(1L, 6)))))
                .isInstanceOf(BusinessRuleException.class);
        verify(saleRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenOneOfSeveralProductsDoesNotExist() {
        when(productRepository.findAllByIdForUpdate(List.of(1L, 9L))).thenReturn(List.of(coffee));

        assertThatThrownBy(() -> saleService.checkout(new SaleRequest(PaymentMethod.PIX, null, null, null,
                List.of(new SaleItemRequest(9L, 1), new SaleItemRequest(1L, 1)))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9");
    }
}
