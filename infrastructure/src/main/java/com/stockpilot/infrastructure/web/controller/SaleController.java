package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.SaleStatus;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase;
import com.stockpilot.core.port.in.ManageSalesUseCase;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SaleSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sales")
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final CheckoutSaleUseCase checkoutService;
    private final ManageSalesUseCase salesService;
    private final ExecuteIdempotentlyUseCase idempotency;

    public SaleController(CheckoutSaleUseCase checkoutService, ManageSalesUseCase salesService,
                          ExecuteIdempotentlyUseCase idempotency) {
        this.checkoutService = checkoutService;
        this.salesService = salesService;
        this.idempotency = idempotency;
    }

    @Operation(summary = "Register a sale. Accepts discountPercentage or discountAmount (not both) and amountReceived for cash",
            description = "Send an Idempotency-Key header to make retries safe: the same key with the same body returns "
                    + "the original sale (header Idempotent-Replayed: true), with a different body it answers 422.")
    @PostMapping
    public ResponseEntity<SaleResponse> checkout(
            @Valid @RequestBody SaleRequest request,
            @RequestHeader(value = IdempotencyHeaders.KEY, required = false) String idempotencyKey) {
        if (idempotencyKey == null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.checkout(request));
        }
        var result = idempotency.execute("sales.checkout", idempotencyKey, request, SaleResponse.class,
                () -> checkoutService.checkout(request));
        return IdempotencyHeaders.created(result);
    }

    @Operation(summary = "Get a sale by id")
    @GetMapping("/{id}")
    public SaleResponse findById(@PathVariable Long id) {
        return salesService.findById(id);
    }

    @Operation(summary = "Search sales by date range, payment method and status with pagination")
    @GetMapping
    public PageResult<SaleResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        return salesService.search(new SaleSearchCriteria(from, to, paymentMethod, status),
                PageQuery.of(page, size, sortBy, direction));
    }

    @Operation(summary = "Cancel a sale and restore its stock; a sale can only be cancelled once")
    @PostMapping("/{id}/cancel")
    public SaleResponse cancel(@PathVariable Long id) {
        return salesService.cancel(id);
    }
}
