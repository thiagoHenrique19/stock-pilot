package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.AdjustStockUseCase;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase;
import com.stockpilot.core.port.in.QueryStockMovementsUseCase;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.StockAdjustmentRequest;
import com.stockpilot.core.port.in.dto.StockMovementResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stock")
@RestController
@RequestMapping("/api/products/{productId}")
public class StockMovementController {

    private final AdjustStockUseCase adjustStockUseCase;
    private final QueryStockMovementsUseCase queryMovementsUseCase;
    private final ExecuteIdempotentlyUseCase idempotency;

    public StockMovementController(AdjustStockUseCase adjustStockUseCase,
                                   QueryStockMovementsUseCase queryMovementsUseCase,
                                   ExecuteIdempotentlyUseCase idempotency) {
        this.idempotency = idempotency;
        this.adjustStockUseCase = adjustStockUseCase;
        this.queryMovementsUseCase = queryMovementsUseCase;
    }

    @Operation(summary = "Manually adjust stock up or down with a mandatory reason (never below zero)")
    @PostMapping("/adjustments")
    public ResponseEntity<ProductResponse> adjust(
            @PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request,
            @RequestHeader(value = IdempotencyHeaders.KEY, required = false) String idempotencyKey) {
        if (idempotencyKey == null) {
            return ResponseEntity.ok(adjustStockUseCase.adjust(productId, request));
        }
        return IdempotencyHeaders.ok(idempotency.execute("stock.adjust:" + productId, idempotencyKey, request,
                ProductResponse.class, () -> adjustStockUseCase.adjust(productId, request)));
    }

    @Operation(summary = "List the stock movement ledger of a product, newest first")
    @GetMapping("/movements")
    public PageResult<StockMovementResponse> movements(@PathVariable Long productId,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size) {
        return queryMovementsUseCase.movementsOf(productId, PageQuery.of(page, size, null, null));
    }
}
