package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SaleFilter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository {

    Sale save(Sale sale);

    Optional<Sale> findById(Long id);

    List<Sale> findByCreatedAtBetween(Instant start, Instant end);

    PageResult<Sale> search(SaleFilter filter, PageQuery pageQuery);
}
