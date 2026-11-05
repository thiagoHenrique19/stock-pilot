package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;

public interface CheckoutSaleUseCase {

    SaleResponse checkout(SaleRequest request);
}
