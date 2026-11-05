package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.Product;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    PageResult<Product> search(ProductSearchCriteria criteria, PageQuery pageQuery);

    boolean existsBySku(String sku);

    List<Product> findLowStock();
}
