package com.stockpilot.application.service;

import com.stockpilot.core.domain.AuditEntry;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.QueryProductHistoryUseCase;
import com.stockpilot.core.port.in.dto.AuditEntryResponse;
import com.stockpilot.core.port.out.AuditLogRepository;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductHistoryService implements QueryProductHistoryUseCase {

    private final ProductRepository productRepository;
    private final AuditLogRepository auditLog;

    public ProductHistoryService(ProductRepository productRepository, AuditLogRepository auditLog) {
        this.productRepository = productRepository;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditEntryResponse> historyOf(Long productId, PageQuery pageQuery) {
        productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return auditLog.findByEntity("Product", productId, pageQuery).map(ProductHistoryService::toResponse);
    }

    private static AuditEntryResponse toResponse(AuditEntry entry) {
        return new AuditEntryResponse(entry.getId(), entry.getField(), entry.getOldValue(), entry.getNewValue(),
                entry.getChangedBy(), entry.getChangedAt());
    }
}
