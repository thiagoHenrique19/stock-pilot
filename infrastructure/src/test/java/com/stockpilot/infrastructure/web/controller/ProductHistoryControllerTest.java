package com.stockpilot.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.QueryProductHistoryUseCase;
import com.stockpilot.core.port.in.dto.AuditEntryResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.infrastructure.web.support.WithSecurityConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductHistoryController.class)
@WithSecurityConfig
@WithMockUser(roles = "ADMIN")
class ProductHistoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private QueryProductHistoryUseCase history;

    @Test
    void shouldReturnAPageOfChanges() throws Exception {
        when(history.historyOf(eq(4L), any())).thenReturn(new PageResult<>(List.of(new AuditEntryResponse(1L, "price",
                "5.00", "6.00", "alice@shop.com", Instant.parse("2026-11-05T10:00:00Z"))), 0, 20, 1));

        mockMvc.perform(get("/api/products/4/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].field").value("price"))
                .andExpect(jsonPath("$.content[0].oldValue").value("5.00"))
                .andExpect(jsonPath("$.content[0].changedBy").value("alice@shop.com"));
    }

    @Test
    void shouldPassPagingParametersAlong() throws Exception {
        when(history.historyOf(eq(4L), any())).thenReturn(new PageResult<>(List.of(), 2, 10, 0));

        mockMvc.perform(get("/api/products/4/history?page=2&size=10")).andExpect(status().isOk());

        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(history).historyOf(eq(4L), captor.capture());
        assertThat(captor.getValue().page()).isEqualTo(2);
        assertThat(captor.getValue().size()).isEqualTo(10);
    }

    @Test
    void shouldAnswer404ForAnUnknownProduct() throws Exception {
        when(history.historyOf(eq(9L), any())).thenThrow(new ResourceNotFoundException("Product", 9L));

        mockMvc.perform(get("/api/products/9/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found with id 9"));
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void shouldBeForbiddenForCashiers() throws Exception {
        mockMvc.perform(get("/api/products/4/history")).andExpect(status().isForbidden());
    }
}
