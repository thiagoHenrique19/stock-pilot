package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final CheckoutSaleUseCase saleService;

    public SaleController(CheckoutSaleUseCase saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse checkout(@Valid @RequestBody SaleRequest request) {
        return saleService.checkout(request);
    }
}
