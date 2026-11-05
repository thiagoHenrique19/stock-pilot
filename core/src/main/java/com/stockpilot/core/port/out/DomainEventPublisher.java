package com.stockpilot.core.port.out;

public interface DomainEventPublisher {

    void publish(Object event);
}
