package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.AuditEntryResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;

public interface QueryProductHistoryUseCase {

    PageResult<AuditEntryResponse> historyOf(Long productId, PageQuery pageQuery);
}
