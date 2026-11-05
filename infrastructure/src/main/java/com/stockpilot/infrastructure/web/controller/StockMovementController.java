package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.AdjustStockUseCase;
import com.stockpilot.core.port.in.QueryStockMovementsUseCase;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.StockAdjustmentRequest;
import com.stockpilot.core.port.in.dto.StockMovementResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}")
public class StockMovementController {

    private final AdjustStockUseCase adjustStockUseCase;
    private final QueryStockMovementsUseCase queryMovementsUseCase;

    public StockMovementController(AdjustStockUseCase adjustStockUseCase,
                                   QueryStockMovementsUseCase queryMovementsUseCase) {
        this.adjustStockUseCase = adjustStockUseCase;
        this.queryMovementsUseCase = queryMovementsUseCase;
    }

    @PostMapping("/adjustments")
    public ProductResponse adjust(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return adjustStockUseCase.adjust(productId, request);
    }

    @GetMapping("/movements")
    public PageResult<StockMovementResponse> movements(@PathVariable Long productId,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size) {
        return queryMovementsUseCase.movementsOf(productId, PageQuery.of(page, size, null, null));
    }
}
