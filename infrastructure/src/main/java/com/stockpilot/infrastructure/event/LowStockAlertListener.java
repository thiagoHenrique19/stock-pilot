package com.stockpilot.infrastructure.event;

import com.stockpilot.core.event.LowStockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LowStockAlertListener {

    private static final Logger log = LoggerFactory.getLogger(LowStockAlertListener.class);

    @EventListener
    public void onLowStock(LowStockEvent event) {
        log.warn("Low stock alert: {} ({}) has {} units left (minimum {})",
                event.productName(), event.sku(), event.quantity(), event.minimumQuantity());
    }
}
