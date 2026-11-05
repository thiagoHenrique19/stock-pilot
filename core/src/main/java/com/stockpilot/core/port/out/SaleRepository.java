package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.Sale;
import java.time.Instant;
import java.util.List;

public interface SaleRepository {

    Sale save(Sale sale);

    List<Sale> findByCreatedAtBetween(Instant start, Instant end);
}
