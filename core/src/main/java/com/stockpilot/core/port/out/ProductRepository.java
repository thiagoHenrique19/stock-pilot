package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    boolean existsBySku(String sku);

    List<Product> findLowStock();
}
