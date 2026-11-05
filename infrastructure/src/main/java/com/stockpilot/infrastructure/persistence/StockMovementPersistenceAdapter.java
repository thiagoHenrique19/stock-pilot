package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.StockMovement;
import com.stockpilot.core.port.out.StockMovementRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class StockMovementPersistenceAdapter implements StockMovementRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final JpaStockMovementRepository jpaRepository;

    StockMovementPersistenceAdapter(JpaStockMovementRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        return jpaRepository.save(movement);
    }

    @Override
    public PageResult<StockMovement> findByProductId(Long productId, PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(), NEWEST_FIRST);
        return PageableMapper.toPageResult(jpaRepository.findByProductId(productId, pageable));
    }
}
