package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.port.out.SaleRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SaleFilter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class SalePersistenceAdapter implements SaleRepository {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "createdAt");
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final JpaSaleRepository jpaRepository;

    SalePersistenceAdapter(JpaSaleRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Sale save(Sale sale) {
        return jpaRepository.save(sale);
    }

    @Override
    public Optional<Sale> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Sale> findByCreatedAtBetween(Instant start, Instant end) {
        return jpaRepository.findByCreatedAtBetween(start, end);
    }

    @Override
    public PageResult<Sale> search(SaleFilter filter, PageQuery pageQuery) {
        Pageable pageable = pageQuery.sortBy() == null
                ? PageRequest.of(pageQuery.page(), pageQuery.size(), NEWEST_FIRST)
                : PageableMapper.toPageable(pageQuery, SORTABLE_FIELDS, "createdAt");
        return PageableMapper.toPageResult(jpaRepository.findAll(SaleSpecifications.from(filter), pageable));
    }
}
