package io.github.dekkerding.examples.event;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import lombok.Getter;

/**
 * 发货事件
 */
@Getter
@StreamEvent(streamKey = "shipment_stream", group = "shipment-group")
public class ShipmentCreatedEvent extends DomainEvent {

    private final String shipmentId;
    private final String orderId;
    private final String address;

    public ShipmentCreatedEvent(Object source, String shipmentId, String orderId, String address) {
        super(source);
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.address = address;
    }
}
