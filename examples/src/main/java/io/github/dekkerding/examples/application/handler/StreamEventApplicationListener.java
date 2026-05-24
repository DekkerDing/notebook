package io.github.dekkerding.examples.application.handler;

import io.github.dekkerding.examples.application.EventPublishingService;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

/**
 * 拦截所有带 @StreamEvent 注解的 DomainEvent，转发到 Redis Stream
 */
@Slf4j
public class StreamEventApplicationListener implements ApplicationListener<DomainEvent> {

    private final EventPublishingService publishingService;

    public StreamEventApplicationListener(EventPublishingService publishingService) {
        this.publishingService = publishingService;
    }

    @Override
    public void onApplicationEvent(DomainEvent event) {
        if (!event.getClass().isAnnotationPresent(StreamEvent.class)) {
            log.debug("事件 {} 未标注 @StreamEvent，跳过 Redis Stream 发布", event.getEventType());
            return;
        }

        try {
            publishingService.publish(event);
        } catch (Exception e) {
            log.error("❌ 事件发布到 Redis Stream 失败: {}", event.getEventType(), e);
        }
    }
}
