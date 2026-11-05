package com.stockpilot.application.service;

import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;
import com.stockpilot.application.mapper.SaleMapper;
import com.stockpilot.core.discount.DiscountPolicyFactory;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.Sale;
import com.stockpilot.core.domain.SaleItem;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.ReceiptNumberGenerator;
import com.stockpilot.core.port.out.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService implements CheckoutSaleUseCase {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;
    private final StockLedger stockLedger;
    private final ReceiptNumberGenerator receiptNumberGenerator;

    public SaleService(ProductRepository productRepository,
                       SaleRepository saleRepository,
                       SaleMapper saleMapper,
                       StockLedger stockLedger,
                       ReceiptNumberGenerator receiptNumberGenerator) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleMapper = saleMapper;
        this.stockLedger = stockLedger;
        this.receiptNumberGenerator = receiptNumberGenerator;
    }

    @Override
    @Transactional
    public SaleResponse checkout(SaleRequest request) {
        Sale sale = Sale.open(request.paymentMethod(), receiptNumberGenerator.next());
        for (SaleItemRequest item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));
            sale.addItem(product, item.quantity());
            product.removeStock(item.quantity());
        }
        sale.applyDiscount(DiscountPolicyFactory.create(request.discountPercentage(), request.discountAmount()));
        sale.receivePayment(request.amountReceived());
        Sale saved = saleRepository.save(sale);
        String reference = saved.getReceiptNumber();
        for (SaleItem item : saved.getItems()) {
            stockLedger.record(item.getProduct(), MovementType.OUT, -item.getQuantity(), "Sale", reference);
        }
        return saleMapper.toResponse(saved);
    }
}
