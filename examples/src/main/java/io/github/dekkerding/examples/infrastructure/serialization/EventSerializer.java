package io.github.dekkerding.examples.infrastructure.serialization;

import io.github.dekkerding.examples.domain.event.DomainEvent;

/**
 * 事件序列化接口
 */
public interface EventSerializer {

    /**
     * 将领域事件序列化为 JSON 字符串
     */
    String serialize(DomainEvent event);

    /**
     * 将 JSON 字符串反序列化为领域事件
     */
    DomainEvent deserialize(String payload, String className);
}
