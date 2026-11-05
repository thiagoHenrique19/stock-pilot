package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.AuditEntry;
import com.stockpilot.core.port.out.AuditLogRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditLogPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private AuditLogRepository auditLog;

    @Test
    void shouldReturnOnlyTheEntitysEntriesNewestFirstAndPaginated() throws Exception {
        long entityId = ThreadLocalRandom.current().nextLong(1_000_000, 9_000_000);
        for (int i = 1; i <= 5; i++) {
            auditLog.save(AuditEntry.record("Product", entityId, "price", i, i + 1, "alice@shop.com"));
            Thread.sleep(5);
        }
        auditLog.save(AuditEntry.record("Product", entityId + 1, "price", 1, 2, "alice@shop.com"));
        auditLog.save(AuditEntry.record("Category", entityId, "name", "a", "b", "alice@shop.com"));

        PageResult<AuditEntry> first = auditLog.findByEntity("Product", entityId, PageQuery.of(0, 2, null, null));
        PageResult<AuditEntry> last = auditLog.findByEntity("Product", entityId, PageQuery.of(2, 2, null, null));

        assertThat(first.totalElements()).isEqualTo(5);
        assertThat(first.content()).extracting(AuditEntry::getNewValue).containsExactly("6", "5");
        assertThat(last.content()).extracting(AuditEntry::getNewValue).containsExactly("2");
    }

    @Test
    void shouldBeEmptyForAnEntityWithoutChanges() {
        assertThat(auditLog.findByEntity("Product", -5L, PageQuery.firstPage()).content()).isEmpty();
    }
}
