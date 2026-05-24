package io.github.dekkerding.examples.application;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.domain.exception.EventDispatchException;
import io.github.dekkerding.examples.infrastructure.idempotent.IdempotentConsumer;
import io.github.dekkerding.examples.infrastructure.serialization.EventSerializer;
import io.github.dekkerding.examples.producer.BusinessProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.Map;

/**
 * 事件消费应用服务，反序列化事件并通过 Spring 事件机制本地分发
 * 同时桥接到 BusinessProcessorFactory 保持向后兼容
 * 支持幂等性消费
 */
@Slf4j
public class EventConsumingService {

    private final EventSerializer serializer;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessProcessorFactory processorFactory;
    private final IdempotentConsumer idempotentConsumer;

    public EventConsumingService(
            EventSerializer serializer,
            ApplicationEventPublisher eventPublisher,
            BusinessProcessorFactory processorFactory) {
        this(serializer, eventPublisher, processorFactory, null);
    }

    public EventConsumingService(
            EventSerializer serializer,
            ApplicationEventPublisher eventPublisher,
            BusinessProcessorFactory processorFactory,
            IdempotentConsumer idempotentConsumer) {
        this.serializer = serializer;
        this.eventPublisher = eventPublisher;
        this.processorFactory = processorFactory;
        this.idempotentConsumer = idempotentConsumer;
    }

    /**
     * 分发事件: 反序列化 → 本地发布 → 桥接旧处理器
     */
    public void dispatch(EventEnvelope envelope) {
        dispatch(envelope, null, null);
    }

    /**
     * 分发事件（带消费者组信息，用于幂等性检查）
     */
    public void dispatch(EventEnvelope envelope, String consumerGroup, String streamKey) {
        String eventClassName = envelope.getEventClassName();
        String payload = envelope.getPayload();

        try {
            // 反序列化为领域事件
            DomainEvent event = serializer.deserialize(payload, eventClassName);

            // 通过 Spring ApplicationEventPublisher 本地发布
            eventPublisher.publishEvent(event);
            log.info("📬 事件已本地分发: type={}, eventId={}",
                    envelope.getMetadata().getEventType(),
                    envelope.getMetadata().getEventId());

            // 桥接到 BusinessProcessorFactory (向后兼容)
            bridgeToBusinessProcessor(envelope);

        } catch (Exception e) {
            throw new EventDispatchException(
                    "事件分发失败: " + eventClassName, e);
        }
    }

    private void bridgeToBusinessProcessor(EventEnvelope envelope) {
        if (processorFactory == null) {
            return;
        }

        try {
            Map<String, String> message = new HashMap<>(envelope.toStreamRecord());
            message.put("type", envelope.getMetadata().getEventType());
            processorFactory.processMessage(message);
        } catch (Exception e) {
            log.debug("BusinessProcessor 桥接处理跳过: {}", e.getMessage());
        }
    }

    public IdempotentConsumer getIdempotentConsumer() {
        return idempotentConsumer;
    }
}
