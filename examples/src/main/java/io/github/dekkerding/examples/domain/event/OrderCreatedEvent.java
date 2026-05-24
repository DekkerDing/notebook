package io.github.dekkerding.examples.domain.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单创建事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends DomainEvent {

    private String orderId;
    private String customerId;
    private BigDecimal amount;

    @Builder
    public OrderCreatedEvent(Object source, String orderId, String customerId, BigDecimal amount) {
        super(source);
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
    }
}
