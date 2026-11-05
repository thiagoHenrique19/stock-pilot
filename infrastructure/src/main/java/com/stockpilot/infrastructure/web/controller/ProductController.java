package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.ProductUpdateRequest;
import com.stockpilot.core.port.in.dto.RestockRequest;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Products")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ManageProductsUseCase productService;
    private final ExecuteIdempotentlyUseCase idempotency;

    public ProductController(ManageProductsUseCase productService, ExecuteIdempotentlyUseCase idempotency) {
        this.idempotency = idempotency;
        this.productService = productService;
    }

    @Operation(summary = "Register a new product (SKU is normalised to upper case)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse register(@Valid @RequestBody ProductRequest request) {
        return productService.register(request);
    }

    @Operation(summary = "Update product details, price, minimum stock and category")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(id, request);
    }

    @Operation(summary = "Deactivate a product (soft delete); inactive products cannot be sold")
    @PostMapping("/{id}/deactivate")
    public ProductResponse deactivate(@PathVariable Long id) {
        return productService.deactivate(id);
    }

    @Operation(summary = "Reactivate a previously deactivated product")
    @PostMapping("/{id}/reactivate")
    public ProductResponse reactivate(@PathVariable Long id) {
        return productService.reactivate(id);
    }

    @Operation(summary = "Add units to the stock of a product")
    @PostMapping("/{id}/restock")
    public ResponseEntity<ProductResponse> restock(
            @PathVariable Long id, @Valid @RequestBody RestockRequest request,
            @RequestHeader(value = IdempotencyHeaders.KEY, required = false) String idempotencyKey) {
        if (idempotencyKey == null) {
            return ResponseEntity.ok(productService.restock(id, request));
        }
        return IdempotencyHeaders.ok(idempotency.execute("products.restock:" + id, idempotencyKey, request,
                ProductResponse.class, () -> productService.restock(id, request)));
    }

    @Operation(summary = "Get a product by id")
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @Operation(summary = "Search products with filters, pagination and sorting (sortBy: id, sku, name, price, quantity)")
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

    @Operation(summary = "List products at or below their minimum stock")
    @GetMapping("/low-stock")
    public List<ProductResponse> listLowStock() {
        return productService.listLowStock();
    }
}
