package com.stockpilot.application.mapper;

import com.stockpilot.core.port.in.dto.SaleItemResponse;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.domain.SaleItem;
import org.springframework.stereotype.Component;

@Component
public class SaleMapper {

    public SaleResponse toResponse(Sale sale) {
        return new SaleResponse(sale.getId(), sale.getPaymentMethod(),
                sale.getItems().stream().map(this::toItemResponse).toList(),
                sale.getSubtotal(), sale.getDiscountAmount(), sale.getTotal(), sale.getCreatedAt());
    }

    private SaleItemResponse toItemResponse(SaleItem item) {
        return new SaleItemResponse(item.getProduct().getId(), item.getProduct().getName(),
                item.getQuantity(), item.getUnitPrice(), item.subtotal());
    }
}
