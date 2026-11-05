package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.query.SaleFilter;
import org.springframework.data.jpa.domain.Specification;

final class SaleSpecifications {

    private SaleSpecifications() {
    }

    static Specification<Sale> from(SaleFilter filter) {
        Specification<Sale> spec = Specification.where(null);
        if (filter.from() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
        }
        if (filter.to() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), filter.to()));
        }
        if (filter.paymentMethod() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentMethod"), filter.paymentMethod()));
        }
        if (filter.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        return spec;
    }
}
