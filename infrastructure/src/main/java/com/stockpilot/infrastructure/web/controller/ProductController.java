package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.RestockRequest;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ManageProductsUseCase productService;

    public ProductController(ManageProductsUseCase productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse register(@Valid @RequestBody ProductRequest request) {
        return productService.register(request);
    }

    @PostMapping("/{id}/restock")
    public ProductResponse restock(@PathVariable Long id, @Valid @RequestBody RestockRequest request) {
        return productService.restock(id, request);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @GetMapping
    public List<ProductResponse> listAll() {
        return productService.listAll();
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> listLowStock() {
        return productService.listLowStock();
    }
}
