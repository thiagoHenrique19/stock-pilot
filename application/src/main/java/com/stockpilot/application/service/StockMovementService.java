package com.stockpilot.application.service;

import com.stockpilot.application.mapper.StockMovementMapper;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.AdjustStockUseCase;
import com.stockpilot.core.port.in.QueryStockMovementsUseCase;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.StockAdjustmentRequest;
import com.stockpilot.core.port.in.dto.StockMovementResponse;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.port.out.StockMovementRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.application.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService implements AdjustStockUseCase, QueryStockMovementsUseCase {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final StockLedger stockLedger;
    private final ProductMapper productMapper;
    private final StockMovementMapper movementMapper;

    public StockMovementService(ProductRepository productRepository,
                                StockMovementRepository movementRepository,
                                StockLedger stockLedger,
                                ProductMapper productMapper,
                                StockMovementMapper movementMapper) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.stockLedger = stockLedger;
        this.productMapper = productMapper;
        this.movementMapper = movementMapper;
    }

    @Override
    @Transactional
    public ProductResponse adjust(Long productId, StockAdjustmentRequest request) {
        Product product = getProduct(productId);
        product.adjustStock(request.quantity());
        stockLedger.record(product, MovementType.ADJUSTMENT, request.quantity(), request.reason(), null);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StockMovementResponse> movementsOf(Long productId, PageQuery pageQuery) {
        getProduct(productId);
        return movementRepository.findByProductId(productId, pageQuery).map(movementMapper::toResponse);
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }
}
