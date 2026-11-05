package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.application.support.ProductFixtures;
import com.stockpilot.core.domain.AuditEntry;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.dto.AuditEntryResponse;
import com.stockpilot.core.port.out.AuditLogRepository;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductHistoryServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private AuditLogRepository auditLog;

    @Test
    void shouldMapEntriesAndKeepThePaginationOfTheRepository() {
        ProductHistoryService service = new ProductHistoryService(productRepository, auditLog);
        PageQuery page = PageQuery.of(1, 5, null, null);
        when(productRepository.findById(3L)).thenReturn(Optional.of(ProductFixtures.product(3L, "H-1", 1)));
        when(auditLog.findByEntity("Product", 3L, page)).thenReturn(new PageResult<>(
                List.of(AuditEntry.record("Product", 3L, "price", "1.00", "2.00", "alice@shop.com")), 1, 5, 6));

        PageResult<AuditEntryResponse> result = service.historyOf(3L, page);

        assertThat(result.totalElements()).isEqualTo(6);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(entry -> {
            assertThat(entry.field()).isEqualTo("price");
            assertThat(entry.oldValue()).isEqualTo("1.00");
            assertThat(entry.newValue()).isEqualTo("2.00");
            assertThat(entry.changedBy()).isEqualTo("alice@shop.com");
        });
    }

    @Test
    void shouldFailForAnUnknownProductWithoutQueryingTheLog() {
        ProductHistoryService service = new ProductHistoryService(productRepository, auditLog);
        when(productRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.historyOf(9L, PageQuery.firstPage()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(auditLog, never()).findByEntity(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
