package com.stockpilot.application.service;

import com.stockpilot.core.domain.MovementType;
import com.stockpilot.core.domain.Product;
import com.stockpilot.core.domain.StockMovement;
import com.stockpilot.core.event.LowStockEvent;
import com.stockpilot.core.port.out.DomainEventPublisher;
import com.stockpilot.core.port.out.StockMovementRepository;
import org.springframework.stereotype.Component;

/**
 * Records a ledger entry for a stock change that was already applied to the product and raises
 * the low stock event when an outgoing movement leaves the product at or below its minimum.
 * It must be called inside the transaction that changed the stock.
 */
@Component
public class StockLedger {

    private final StockMovementRepository movementRepository;
    private final DomainEventPublisher eventPublisher;

    public StockLedger(StockMovementRepository movementRepository, DomainEventPublisher eventPublisher) {
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
    }

    public StockMovement record(Product product, MovementType type, int quantity, String reason, String reference) {
        StockMovement movement = movementRepository.save(
                StockMovement.record(product, type, quantity, reason, reference));
        if (quantity < 0 && product.isLowStock()) {
            eventPublisher.publish(new LowStockEvent(product.getId(), product.getSku(),
                    product.getName(), product.getQuantity(), product.getMinimumQuantity()));
        }
        return movement;
    }
}
