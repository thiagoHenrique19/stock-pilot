package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Product;
import com.stockpilot.core.query.ProductSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> from(ProductSearchCriteria criteria) {
        Specification<Product> spec = Specification.where(null);
        if (criteria.nameContains() != null && !criteria.nameContains().isBlank()) {
            spec = spec.and(nameContains(criteria.nameContains().trim()));
        }
        if (criteria.categoryId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), criteria.categoryId()));
        }
        if (criteria.active() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), criteria.active()));
        }
        if (criteria.lowStockOnly()) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("quantity"), root.get("minimumQuantity")));
        }
        return spec;
    }

    private static Specification<Product> nameContains(String text) {
        String escaped = text.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\');
    }
}
