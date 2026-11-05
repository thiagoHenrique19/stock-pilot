package com.stockpilot.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AuditEntryTest {

    @Test
    void shouldKeepOldAndNewValuesAsText() {
        AuditEntry entry = AuditEntry.record("Product", 7L, "price", new BigDecimal("5.00"), new BigDecimal("6.50"),
                "alice@shop.com");

        assertThat(entry.getOldValue()).isEqualTo("5.00");
        assertThat(entry.getNewValue()).isEqualTo("6.50");
        assertThat(entry.getChangedAt()).isNotNull();
    }

    @Test
    void shouldAllowAMissingOldValue() {
        AuditEntry entry = AuditEntry.record("Product", 7L, "active", null, true, "alice@shop.com");

        assertThat(entry.getOldValue()).isNull();
        assertThat(entry.getNewValue()).isEqualTo("true");
    }

    @Test
    void shouldRejectEntriesWhereNothingChanged() {
        assertThatThrownBy(() -> AuditEntry.record("Product", 7L, "active", true, true, "alice@shop.com"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {"'',price,alice", "Product,'',alice", "Product,price,''", " ,price,alice"})
    void shouldRequireEntityFieldAndUser(String type, String field, String user) {
        assertThatThrownBy(() -> AuditEntry.record(type, 7L, field, "1", "2", user))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRequireAnEntityId() {
        assertThatThrownBy(() -> AuditEntry.record("Product", null, "price", "1", "2", "alice"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
