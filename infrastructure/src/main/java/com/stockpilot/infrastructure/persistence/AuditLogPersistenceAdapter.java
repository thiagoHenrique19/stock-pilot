package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.AuditEntry;
import com.stockpilot.core.port.out.AuditLogRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class AuditLogPersistenceAdapter implements AuditLogRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("id"));

    private final JpaAuditLogRepository jpaRepository;

    AuditLogPersistenceAdapter(JpaAuditLogRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditEntry save(AuditEntry entry) {
        return jpaRepository.save(entry);
    }

    @Override
    public PageResult<AuditEntry> findByEntity(String entityType, Long entityId, PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(), NEWEST_FIRST);
        return PageableMapper.toPageResult(jpaRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable));
    }
}
