package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    @Query("select p from Product p where p.quantity <= p.minimumQuantity order by p.quantity")
    List<Product> findLowStock();
}
