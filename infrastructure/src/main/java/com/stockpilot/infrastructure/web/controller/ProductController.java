package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.ManageProductsUseCase;
import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.ProductUpdateRequest;
import com.stockpilot.core.port.in.dto.RestockRequest;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public ProductResponse deactivate(@PathVariable Long id) {
        return productService.deactivate(id);
    }

    @PostMapping("/{id}/reactivate")
    public ProductResponse reactivate(@PathVariable Long id) {
        return productService.reactivate(id);
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
    public PageResult<ProductResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        return productService.search(new ProductSearchCriteria(name, categoryId, active, lowStockOnly),
                PageQuery.of(page, size, sortBy, direction));
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> listLowStock() {
        return productService.listLowStock();
    }
}
