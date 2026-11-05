package com.stockpilot.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.AdjustStockUseCase;
import com.stockpilot.core.port.in.QueryStockMovementsUseCase;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.StockMovementResponse;
import com.stockpilot.core.query.PageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockMovementController.class)
class StockMovementControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AdjustStockUseCase adjustStockUseCase;
    @MockBean private QueryStockMovementsUseCase queryMovementsUseCase;

    private final ProductResponse product = new ProductResponse(1L, "SKU-1", "Coffee", null, null, null,
            new BigDecimal("19.90"), 8, 3, false, true);

    @Test
    void shouldAdjustStock() throws Exception {
        when(adjustStockUseCase.adjust(eq(1L), any())).thenReturn(product);

        mockMvc.perform(post("/api/products/1/adjustments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-2,\"reason\":\"Damaged\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(8));
    }

    @Test
    void shouldRequireReasonAndQuantity() throws Exception {
        mockMvc.perform(post("/api/products/1/adjustments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-2,\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/products/1/adjustments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnprocessableEntityWhenAdjustmentGoesBelowZero() throws Exception {
        when(adjustStockUseCase.adjust(eq(1L), any())).thenThrow(new BusinessRuleException("below zero"));

        mockMvc.perform(post("/api/products/1/adjustments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-99,\"reason\":\"Loss\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturnNotFoundForUnknownProduct() throws Exception {
        when(adjustStockUseCase.adjust(eq(9L), any())).thenThrow(new ResourceNotFoundException("Product", 9L));

        mockMvc.perform(post("/api/products/9/adjustments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1,\"reason\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListMovementsPaginated() throws Exception {
        StockMovementResponse movement = new StockMovementResponse(5L, 1L, MovementType.IN, 10, 10,
                "Restock", null, Instant.parse("2026-11-05T10:00:00Z"));
        when(queryMovementsUseCase.movementsOf(eq(1L), any())).thenReturn(new PageResult<>(List.of(movement), 0, 20, 1));

        mockMvc.perform(get("/api/products/1/movements").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("IN"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
