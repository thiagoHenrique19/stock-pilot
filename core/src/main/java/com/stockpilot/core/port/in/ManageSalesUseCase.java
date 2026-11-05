package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SaleSearchCriteria;

public interface ManageSalesUseCase {

    SaleResponse findById(Long id);

    PageResult<SaleResponse> search(SaleSearchCriteria criteria, PageQuery pageQuery);

    SaleResponse cancel(Long id);
}
