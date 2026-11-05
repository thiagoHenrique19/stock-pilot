package com.stockpilot.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.SaleStatus;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase;
import com.stockpilot.core.port.in.ManageSalesUseCase;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SaleSearchCriteria;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.stockpilot.infrastructure.web.support.WithSecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SaleController.class)
@WithSecurityConfig
@WithMockUser(roles = "ADMIN")
class SaleControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CheckoutSaleUseCase checkoutService;
    @MockBean private ManageSalesUseCase salesService;
    @MockBean private ExecuteIdempotentlyUseCase idempotency;

    private final SaleResponse sale = new SaleResponse(7L, "SALE-000007", SaleStatus.COMPLETED, PaymentMethod.CASH,
            List.of(), new BigDecimal("40.00"), BigDecimal.ZERO, new BigDecimal("40.00"),
            new BigDecimal("50.00"), new BigDecimal("10.00"), Instant.parse("2026-11-05T10:00:00Z"), null);

    @Test
    void shouldCheckoutWithCashAmountAndReturnChange() throws Exception {
        when(checkoutService.checkout(any())).thenReturn(sale);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","amountReceived":50.00,"discountAmount":0,
                                 "items":[{"productId":1,"quantity":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("SALE-000007"))
                .andExpect(jsonPath("$.change").value(10.0));
    }

    private static final String BODY = "{\"paymentMethod\":\"PIX\",\"items\":[{\"productId\":1,\"quantity\":2}]}";

    @Test
    void shouldNotTouchTheIdempotencyStoreWhenNoKeyIsSent() throws Exception {
        when(checkoutService.checkout(any())).thenReturn(sale);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));

        verifyNoInteractions(idempotency);
    }

    @Test
    void shouldRunCheckoutThroughTheIdempotencyUseCaseWhenAKeyIsSent() throws Exception {
        when(idempotency.execute(eq("sales.checkout"), eq("key-1"), any(), eq(SaleResponse.class), any()))
                .thenReturn(new ExecuteIdempotentlyUseCase.Result<>(sale, false));

        mockMvc.perform(post("/api/sales").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(jsonPath("$.receiptNumber").value("SALE-000007"));
    }

    @Test
    void shouldFlagReplayedResponsesWithAHeader() throws Exception {
        when(idempotency.execute(eq("sales.checkout"), eq("key-1"), any(), eq(SaleResponse.class), any()))
                .thenReturn(new ExecuteIdempotentlyUseCase.Result<>(sale, true));

        mockMvc.perform(post("/api/sales").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"));
    }

    @Test
    void shouldValidateTheBodyBeforeLookingAtTheKey() throws Exception {
        mockMvc.perform(post("/api/sales").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"PIX\",\"items\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(idempotency);
    }

    @Test
    void shouldRejectSaleWithoutItems() throws Exception {
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CASH\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnknownPaymentMethod() throws Exception {
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"BARTER\",\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetSaleById() throws Exception {
        when(salesService.findById(7L)).thenReturn(sale);

        mockMvc.perform(get("/api/sales/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturnNotFoundForUnknownSale() throws Exception {
        when(salesService.findById(9L)).thenThrow(new ResourceNotFoundException("Sale", 9L));

        mockMvc.perform(get("/api/sales/9")).andExpect(status().isNotFound());
    }

    @Test
    void shouldCancelSale() throws Exception {
        when(salesService.cancel(7L)).thenReturn(sale);

        mockMvc.perform(post("/api/sales/7/cancel")).andExpect(status().isOk());
        verify(salesService).cancel(7L);
    }

    @Test
    void shouldReturnUnprocessableEntityWhenCancellingTwice() throws Exception {
        when(salesService.cancel(7L)).thenThrow(new BusinessRuleException("Sale SALE-000007 is already cancelled"));

        mockMvc.perform(post("/api/sales/7/cancel"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Sale SALE-000007 is already cancelled"));
    }

    @Test
    void shouldReturnConflictOnConcurrentCancellation() throws Exception {
        when(salesService.cancel(7L)).thenThrow(new ObjectOptimisticLockingFailureException("Sale", 7L));

        mockMvc.perform(post("/api/sales/7/cancel")).andExpect(status().isConflict());
    }

    @Test
    void shouldSearchSalesWithFilters() throws Exception {
        when(salesService.search(any(), any())).thenReturn(new PageResult<>(List.of(sale), 0, 10, 1));

        mockMvc.perform(get("/api/sales").param("from", "2026-11-01").param("to", "2026-11-05")
                        .param("paymentMethod", "CASH").param("status", "COMPLETED")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].receiptNumber").value("SALE-000007"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<SaleSearchCriteria> captor = ArgumentCaptor.forClass(SaleSearchCriteria.class);
        ArgumentCaptor<PageQuery> pageCaptor = ArgumentCaptor.forClass(PageQuery.class);
        verify(salesService).search(captor.capture(), pageCaptor.capture());
        assertThat(captor.getValue()).isEqualTo(new SaleSearchCriteria(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 5), PaymentMethod.CASH, SaleStatus.COMPLETED));
        assertThat(pageCaptor.getValue().size()).isEqualTo(10);
    }

    @Test
    void shouldRejectInvalidFilterValues() throws Exception {
        mockMvc.perform(get("/api/sales").param("status", "UNKNOWN")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/sales").param("from", "not-a-date")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvertedDateRange() throws Exception {
        mockMvc.perform(get("/api/sales").param("from", "2026-11-05").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }
}
