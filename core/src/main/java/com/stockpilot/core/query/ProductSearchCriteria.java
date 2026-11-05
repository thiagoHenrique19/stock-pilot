package com.stockpilot.core.query;

public record ProductSearchCriteria(String nameContains, Long categoryId, Boolean active, boolean lowStockOnly) {

    public static ProductSearchCriteria all() {
        return new ProductSearchCriteria(null, null, null, false);
    }
}
