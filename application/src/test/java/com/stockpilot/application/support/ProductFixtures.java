package com.stockpilot.application.support;

import com.stockpilot.core.domain.Product;
import java.lang.reflect.Field;
import java.math.BigDecimal;

public final class ProductFixtures {

    private ProductFixtures() {
    }

    public static Product product(Long id, String sku, int quantity) {
        Product product = Product.create(sku, "Product " + sku, new BigDecimal("10.00"), quantity, 1);
        setId(product, id);
        return product;
    }

    public static void setId(Product product, Long id) {
        try {
            Field field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(product, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
