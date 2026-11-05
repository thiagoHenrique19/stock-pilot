package com.stockpilot.application.service;

import com.stockpilot.application.mapper.ProductMapper;
import com.stockpilot.core.domain.AuditEntry;
import com.stockpilot.core.domain.Category;
import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.ProductUpdateRequest;
import com.stockpilot.core.port.in.dto.RestockRequest;
import com.stockpilot.core.port.out.AuditLogRepository;
import com.stockpilot.core.port.out.CategoryRepository;
import com.stockpilot.core.port.out.CurrentUserProvider;
import com.stockpilot.core.port.out.ProductRepository;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService implements ManageProductsUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockLedger stockLedger;
    private final ProductMapper productMapper;
    private final AuditLogRepository auditLog;
    private final CurrentUserProvider currentUser;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          StockLedger stockLedger,
                          ProductMapper productMapper,
                          AuditLogRepository auditLog,
                          CurrentUserProvider currentUser) {
        this.auditLog = auditLog;
        this.currentUser = currentUser;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.stockLedger = stockLedger;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public ProductResponse register(ProductRequest request) {
        String sku = Product.normalizeSku(request.sku());
        if (productRepository.existsBySku(sku)) {
            throw new BusinessRuleException("SKU already registered: " + sku);
        }
        Category category = resolveCategory(request.categoryId());
        Product product = productRepository.save(productMapper.toEntity(request, category));
        if (product.getQuantity() > 0) {
            stockLedger.record(product, MovementType.IN, product.getQuantity(), "Initial stock", null);
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = getProduct(id);
        BigDecimal oldPrice = product.getPrice();
        product.update(request.name(), request.description(), request.price(),
                request.minimumQuantity(), resolveCategory(request.categoryId()));
        if (oldPrice.compareTo(product.getPrice()) != 0) {
            audit(product, "price", oldPrice.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    product.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse deactivate(Long id) {
        Product product = getProduct(id);
        boolean wasActive = product.isActive();
        product.deactivate();
        if (wasActive) {
            audit(product, "active", true, false);
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse reactivate(Long id) {
        Product product = getProduct(id);
        boolean wasActive = product.isActive();
        product.reactivate();
        if (!wasActive) {
            audit(product, "active", false, true);
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse restock(Long id, RestockRequest request) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.addStock(request.quantity());
        stockLedger.record(product, MovementType.IN, request.quantity(), "Restock", null);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(getProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductResponse> search(ProductSearchCriteria criteria, PageQuery pageQuery) {
        return productRepository.search(criteria, pageQuery).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> listLowStock() {
        return productRepository.findLowStock().stream().map(productMapper::toResponse).toList();
    }

    private void audit(Product product, String field, Object oldValue, Object newValue) {
        auditLog.save(AuditEntry.record("Product", product.getId(), field, oldValue, newValue,
                currentUser.currentUserOrSystem()));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }
}
