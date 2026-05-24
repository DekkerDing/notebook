package io.github.dekkerding.examples.domain.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 支付完成事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentCompletedEvent extends DomainEvent {

    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;

    @Builder
    public PaymentCompletedEvent(Object source, String paymentId, String orderId, BigDecimal amount, String paymentMethod) {
        super(source);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }
}
