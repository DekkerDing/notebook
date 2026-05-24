package io.github.dekkerding.examples.application;

import io.github.dekkerding.examples.config.RedisStreamEventProperties;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.infrastructure.publisher.EventPublisher;
import io.github.dekkerding.examples.infrastructure.publisher.PartitionedStreamPublisher;
import io.github.dekkerding.examples.infrastructure.publisher.PartitionedStreamPublisher.PartitionedMessageId;
import io.github.dekkerding.examples.infrastructure.routing.EventStreamRouter;
import io.github.dekkerding.examples.infrastructure.serialization.EventSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * 事件发布应用服务，编排序列化、路由和发布流程
 * 支持普通模式和分区模式
 */
@Slf4j
public class EventPublishingService {

    private final EventSerializer serializer;
    private final EventStreamRouter router;
    private final EventPublisher publisher;
    private final PartitionedStreamPublisher partitionedPublisher;
    private final RedisStreamEventProperties properties;

    public EventPublishingService(
            EventSerializer serializer,
            EventStreamRouter router,
            EventPublisher publisher) {
        this(serializer, router, publisher, null, null);
    }

    public EventPublishingService(
            EventSerializer serializer,
            EventStreamRouter router,
            EventPublisher publisher,
            PartitionedStreamPublisher partitionedPublisher,
            RedisStreamEventProperties properties) {
        this.serializer = serializer;
        this.router = router;
        this.publisher = publisher;
        this.partitionedPublisher = partitionedPublisher;
        this.properties = properties;
    }

    public EventPublisher.MessageId publish(DomainEvent event) {
        String streamKey = router.resolveStreamKey(event.getClass());
        String payload = serializer.serialize(event);

        EventEnvelope envelope = EventEnvelope.builder()
                .metadata(event.getMetadata())
                .payload(payload)
                .eventClassName(event.getClass().getName())
                .build();

        if (properties != null && properties.getPartition().isEnabled() && partitionedPublisher != null) {
            return publishToPartition(envelope);
        }

        log.debug("发布事件: type={}, streamKey={}", event.getEventType(), streamKey);
        return publisher.publish(envelope, streamKey);
    }

    private EventPublisher.MessageId publishToPartition(EventEnvelope envelope) {
        PartitionedMessageId partitionedId = partitionedPublisher.publish(envelope);
        log.info("📤 发布到分区: partition={}, stream={}, eventId={}",
                partitionedId.getPartition(),
                partitionedId.getStreamKey(),
                envelope.getMetadata().getEventId());
        return EventPublisher.MessageId.of(partitionedId.getRecordId());
    }
}
