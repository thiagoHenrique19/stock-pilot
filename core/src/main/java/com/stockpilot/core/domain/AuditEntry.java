package com.stockpilot.core.domain;

import com.stockpilot.core.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String field;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    protected AuditEntry() {
    }

    private AuditEntry(String entityType, Long entityId, String field, String oldValue, String newValue,
                       String changedBy) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
    }

    public static AuditEntry record(String entityType, Long entityId, String field, Object oldValue,
                                    Object newValue, String changedBy) {
        if (entityType == null || entityType.isBlank() || field == null || field.isBlank()
                || changedBy == null || changedBy.isBlank() || entityId == null) {
            throw new BusinessRuleException("An audit entry needs an entity, a field and the user who changed it");
        }
        if (Objects.equals(oldValue, newValue)) {
            throw new BusinessRuleException("Nothing changed for " + field);
        }
        return new AuditEntry(entityType, entityId, field, text(oldValue), text(newValue), changedBy);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getField() {
        return field;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
