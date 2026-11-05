package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaAuditLogRepository extends JpaRepository<AuditEntry, Long> {

    Page<AuditEntry> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
}
