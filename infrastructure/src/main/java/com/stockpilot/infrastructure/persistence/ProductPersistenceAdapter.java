package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Product;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
class ProductPersistenceAdapter implements ProductRepository {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "sku", "name", "price", "quantity");

    private final JpaProductRepository jpaRepository;
    private final EntityManager entityManager;

    ProductPersistenceAdapter(JpaProductRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
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
    public List<Product> findAllByIdForUpdate(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Product> locked = jpaRepository.lockAllByIdOrderById(ids);
        // Hibernate keeps the state of entities that are already in the persistence context and
        // only takes the row lock, so refresh to see what a concurrent commit changed.
        locked.forEach(product -> entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE));
        return locked;
    }

    @Override
    public PageResult<Product> search(ProductSearchCriteria criteria, PageQuery pageQuery) {
        Pageable pageable = PageableMapper.toPageable(pageQuery, SORTABLE_FIELDS, "id");
        return PageableMapper.toPageResult(jpaRepository.findAll(ProductSpecifications.from(criteria), pageable));
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
