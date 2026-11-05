package com.stockpilot.application.mapper;

import com.stockpilot.core.domain.Category;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request, Category category) {
        return Product.create(request.sku(), request.name(), request.description(), request.price(),
                request.quantity(), request.minimumQuantity(), category);
    }

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                category == null ? null : category.getId(), category == null ? null : category.getName(),
                product.getPrice(), product.getQuantity(), product.getMinimumQuantity(),
                product.isLowStock(), product.isActive());
    }
}
