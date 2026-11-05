package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.port.out.SaleRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class SalePersistenceAdapter implements SaleRepository {

    private final JpaSaleRepository jpaRepository;

    SalePersistenceAdapter(JpaSaleRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Sale save(Sale sale) {
        return jpaRepository.save(sale);
    }

    @Override
    public List<Sale> findByCreatedAtBetween(Instant start, Instant end) {
        return jpaRepository.findByCreatedAtBetween(start, end);
    }
}
