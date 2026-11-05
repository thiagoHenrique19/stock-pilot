package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Sale;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface JpaSaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    List<Sale> findByCreatedAtBetween(Instant start, Instant end);
}
