package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.Product;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    /**
     * Loads the products holding a write lock until the surrounding transaction ends. Rows are
     * always locked in ascending id order so concurrent callers cannot deadlock each other.
     * Unknown ids are skipped and duplicates are collapsed.
     */
    List<Product> findAllByIdForUpdate(Collection<Long> ids);

    default Optional<Product> findByIdForUpdate(Long id) {
        return findAllByIdForUpdate(List.of(id)).stream().findFirst();
    }

    PageResult<Product> search(ProductSearchCriteria criteria, PageQuery pageQuery);

    boolean existsBySku(String sku);

    List<Product> findLowStock();
}
