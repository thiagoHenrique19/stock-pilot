package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.StockMovement;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;

public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    PageResult<StockMovement> findByProductId(Long productId, PageQuery pageQuery);
}
