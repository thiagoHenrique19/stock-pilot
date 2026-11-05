package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.StockAdjustmentRequest;

public interface AdjustStockUseCase {

    ProductResponse adjust(Long productId, StockAdjustmentRequest request);
}
