package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.QueryProductHistoryUseCase;
import com.stockpilot.core.port.in.dto.AuditEntryResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Products")
@RestController
@RequestMapping("/api/products/{productId}/history")
public class ProductHistoryController {

    private final QueryProductHistoryUseCase historyUseCase;

    public ProductHistoryController(QueryProductHistoryUseCase historyUseCase) {
        this.historyUseCase = historyUseCase;
    }

    @Operation(summary = "Who changed the price or the active flag of a product and when, newest first (admin only)")
    @GetMapping
    public PageResult<AuditEntryResponse> history(@PathVariable Long productId,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        return historyUseCase.historyOf(productId, PageQuery.of(page, size, null, null));
    }
}
