package io.github.dekkerding.examples.event;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import lombok.Getter;

/**
 * 订单创建事件
 */
@Getter
@StreamEvent(streamKey = "order_stream", group = "order-group")
public class OrderCreatedEvent extends DomainEvent {

    private final String orderId;
    private final String userId;
    private final double amount;
    private final String items;

    public OrderCreatedEvent(Object source, String orderId, String userId, double amount, String items) {
        super(source);
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.items = items;
    }
}
