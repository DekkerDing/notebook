package io.github.dekkerding.examples.event;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import lombok.Getter;

/**
 * 支付失败事件
 */
@Getter
@StreamEvent(streamKey = "payment_stream", group = "payment-group")
public class PaymentFailedEvent extends DomainEvent {

    private final String paymentId;
    private final String orderId;
    private final String reason;

    public PaymentFailedEvent(Object source, String paymentId, String orderId, String reason) {
        super(source);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.reason = reason;
    }
}
