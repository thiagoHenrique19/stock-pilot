package com.stockpilot.application.service;

import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.application.mapper.SaleMapper;
import com.stockpilot.core.discount.DiscountPolicyFactory;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.event.LowStockEvent;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.SaleRepository;
import com.stockpilot.core.port.out.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService implements CheckoutSaleUseCase {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;
    private final DomainEventPublisher eventPublisher;

    public SaleService(ProductRepository productRepository,
                       SaleRepository saleRepository,
                       SaleMapper saleMapper,
                       DomainEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleMapper = saleMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SaleResponse checkout(SaleRequest request) {
        Sale sale = Sale.open(request.paymentMethod());
        for (SaleItemRequest item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));
            sale.addItem(product, item.quantity());
            product.removeStock(item.quantity());
            notifyIfLowStock(product);
        }
        sale.applyDiscount(DiscountPolicyFactory.fromPercentage(request.discountPercentage()));
        return saleMapper.toResponse(saleRepository.save(sale));
    }

    private void notifyIfLowStock(Product product) {
        if (product.isLowStock()) {
            eventPublisher.publish(new LowStockEvent(product.getId(), product.getSku(),
                    product.getName(), product.getQuantity(), product.getMinimumQuantity()));
        }
    }
}
