package com.stockpilot.application.mapper;

import com.stockpilot.core.domain.StockMovement;
import com.stockpilot.core.port.in.dto.StockMovementResponse;
import org.springframework.stereotype.Component;

@Component
public class StockMovementMapper {

    public StockMovementResponse toResponse(StockMovement movement) {
        return new StockMovementResponse(movement.getId(), movement.getProduct().getId(), movement.getType(),
                movement.getQuantity(), movement.getBalanceAfter(), movement.getReason(),
                movement.getReference(), movement.getCreatedAt());
    }
}
