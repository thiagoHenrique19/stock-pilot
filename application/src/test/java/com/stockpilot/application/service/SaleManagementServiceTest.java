package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.application.mapper.SaleMapper;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.domain.SaleStatus;
import com.stockpilot.core.domain.StockMovement;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.core.port.out.DomainEventPublisher;
import com.stockpilot.core.port.out.SaleRepository;
import com.stockpilot.core.port.out.StockMovementRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SaleFilter;
import com.stockpilot.core.query.SaleSearchCriteria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleManagementServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private StockMovementRepository movementRepository;
    @Mock private DomainEventPublisher eventPublisher;

    private SaleManagementService service;
    private Product coffee;
    private Sale sale;

    @BeforeEach
    void setUp() {
        service = new SaleManagementService(saleRepository,
                new StockLedger(movementRepository, eventPublisher), new SaleMapper());
        coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 10, 3);
        sale = Sale.open(PaymentMethod.PIX, "SALE-000007");
        sale.addItem(coffee, 4);
        coffee.removeStock(4);
    }

    @Test
    void shouldCancelSaleRestoringStockAndRecordingMovements() {
        when(saleRepository.findById(7L)).thenReturn(Optional.of(sale));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = service.cancel(7L);

        assertThat(response.status()).isEqualTo(SaleStatus.CANCELLED);
        assertThat(response.cancelledAt()).isNotNull();
        assertThat(coffee.getQuantity()).isEqualTo(10);
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(MovementType.IN);
        assertThat(captor.getValue().getQuantity()).isEqualTo(4);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(10);
        assertThat(captor.getValue().getReference()).isEqualTo("SALE-000007");
    }

    @Test
    void shouldNotCancelTwiceNorRestoreStockAgain() {
        sale.cancel();
        when(saleRepository.findById(7L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> service.cancel(7L)).isInstanceOf(BusinessRuleException.class);

        assertThat(coffee.getQuantity()).isEqualTo(6);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldFailToCancelUnknownSale() {
        when(saleRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(7L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.findById(7L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindSaleById() {
        when(saleRepository.findById(7L)).thenReturn(Optional.of(sale));

        SaleResponse response = service.findById(7L);

        assertThat(response.receiptNumber()).isEqualTo("SALE-000007");
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void shouldTranslateInclusiveDatesIntoInstantRangeAndSearch() {
        PageQuery pageQuery = PageQuery.firstPage();
        when(saleRepository.search(any(), any())).thenReturn(new PageResult<>(List.of(sale), 0, 20, 1));
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 5);

        PageResult<SaleResponse> result = service.search(
                new SaleSearchCriteria(from, to, PaymentMethod.PIX, SaleStatus.COMPLETED), pageQuery);

        assertThat(result.content()).hasSize(1);
        ArgumentCaptor<SaleFilter> captor = ArgumentCaptor.forClass(SaleFilter.class);
        verify(saleRepository).search(captor.capture(), any());
        ZoneId zone = ZoneId.systemDefault();
        assertThat(captor.getValue().from()).isEqualTo(from.atStartOfDay(zone).toInstant());
        assertThat(captor.getValue().to()).isEqualTo(to.plusDays(1).atStartOfDay(zone).toInstant());
        assertThat(captor.getValue().paymentMethod()).isEqualTo(PaymentMethod.PIX);
        assertThat(captor.getValue().status()).isEqualTo(SaleStatus.COMPLETED);
    }

    @Test
    void shouldRejectInvertedDateRange() {
        assertThatThrownBy(() -> new SaleSearchCriteria(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1), null, null))
                .isInstanceOf(InvalidRequestException.class);
    }
}
