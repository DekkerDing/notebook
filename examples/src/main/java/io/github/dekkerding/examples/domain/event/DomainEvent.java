package io.github.dekkerding.examples.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 领域事件基类，所有需要通过 Redis Stream 传输的事件都应继承此类
 */
@Getter
public abstract class DomainEvent extends ApplicationEvent {

    private final EventMetadata metadata;

    protected DomainEvent(Object source) {
        super(source);
        this.metadata = EventMetadata.create(this.getClass());
    }

    protected DomainEvent(Object source, String sourceApplication) {
        super(source);
        this.metadata = EventMetadata.create(this.getClass(), sourceApplication);
    }

    /**
     * 获取事件类型标识，用于消息路由
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
