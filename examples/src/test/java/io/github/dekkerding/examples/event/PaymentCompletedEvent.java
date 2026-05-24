package io.github.dekkerding.examples.event;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import lombok.Getter;

/**
 * 支付完成事件
 */
@Getter
@StreamEvent(streamKey = "payment_stream", group = "payment-group")
public class PaymentCompletedEvent extends DomainEvent {

    private final String paymentId;
    private final String orderId;
    private final double amount;
    private final String paymentMethod;

    public PaymentCompletedEvent(Object source, String paymentId, String orderId,
                                  double amount, String paymentMethod) {
        super(source);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }
}
