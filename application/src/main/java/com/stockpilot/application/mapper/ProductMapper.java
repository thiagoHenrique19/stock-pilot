package com.stockpilot.application.mapper;

import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.domain.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        return Product.create(request.sku(), request.name(), request.price(),
                request.quantity(), request.minimumQuantity());
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getPrice(),
                product.getQuantity(), product.getMinimumQuantity(), product.isLowStock());
    }
}
