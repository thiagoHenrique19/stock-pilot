package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.AuditEntry;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;

public interface AuditLogRepository {

    AuditEntry save(AuditEntry entry);

    /** Newest change first. */
    PageResult<AuditEntry> findByEntity(String entityType, Long entityId, PageQuery pageQuery);
}
