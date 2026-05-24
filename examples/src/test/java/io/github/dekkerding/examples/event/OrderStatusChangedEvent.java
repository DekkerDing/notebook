package io.github.dekkerding.examples.event;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import lombok.Getter;

/**
 * 订单状态变更事件
 */
@Getter
@StreamEvent(streamKey = "order_stream", group = "order-group")
public class OrderStatusChangedEvent extends DomainEvent {

    private final String orderId;
    private final String fromStatus;
    private final String toStatus;

    public OrderStatusChangedEvent(Object source, String orderId, String fromStatus, String toStatus) {
        super(source);
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }
}
