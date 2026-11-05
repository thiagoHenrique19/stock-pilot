package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Product;
import com.stockpilot.core.port.out.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ProductPersistenceAdapter implements ProductRepository {

    private final JpaProductRepository jpaRepository;

    ProductPersistenceAdapter(JpaProductRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpaRepository.existsBySku(sku);
    }

    @Override
    public List<Product> findLowStock() {
        return jpaRepository.findLowStock();
    }
}
