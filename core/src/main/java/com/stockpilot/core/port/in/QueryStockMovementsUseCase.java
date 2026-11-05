package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.StockMovementResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;

public interface QueryStockMovementsUseCase {

    PageResult<StockMovementResponse> movementsOf(Long productId, PageQuery pageQuery);
}
