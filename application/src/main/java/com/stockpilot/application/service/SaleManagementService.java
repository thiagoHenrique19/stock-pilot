package com.stockpilot.application.service;

import com.stockpilot.application.mapper.SaleMapper;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.domain.SaleItem;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.ManageSalesUseCase;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.core.port.out.SaleRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SaleFilter;
import com.stockpilot.core.query.SaleSearchCriteria;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleManagementService implements ManageSalesUseCase {

    private final SaleRepository saleRepository;
    private final StockLedger stockLedger;
    private final SaleMapper saleMapper;

    public SaleManagementService(SaleRepository saleRepository, StockLedger stockLedger, SaleMapper saleMapper) {
        this.saleRepository = saleRepository;
        this.stockLedger = stockLedger;
        this.saleMapper = saleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        return saleMapper.toResponse(getSale(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SaleResponse> search(SaleSearchCriteria criteria, PageQuery pageQuery) {
        ZoneId zone = ZoneId.systemDefault();
        Instant from = criteria.from() == null ? null : criteria.from().atStartOfDay(zone).toInstant();
        Instant to = criteria.to() == null ? null : criteria.to().plusDays(1).atStartOfDay(zone).toInstant();
        SaleFilter filter = new SaleFilter(from, to, criteria.paymentMethod(), criteria.status());
        return saleRepository.search(filter, pageQuery).map(saleMapper::toResponse);
    }

    @Override
    @Transactional
    public SaleResponse cancel(Long id) {
        Sale sale = getSale(id);
        sale.cancel();
        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
            product.addStock(item.getQuantity());
            stockLedger.record(product, MovementType.IN, item.getQuantity(),
                    "Sale cancelled", sale.getReceiptNumber());
        }
        return saleMapper.toResponse(sale);
    }

    private Sale getSale(Long id) {
        return saleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }
}
