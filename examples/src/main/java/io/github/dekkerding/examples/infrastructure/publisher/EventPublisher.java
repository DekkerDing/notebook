package io.github.dekkerding.examples.infrastructure.publisher;

import io.github.dekkerding.examples.domain.event.EventEnvelope;

import java.io.Serializable;

/**
 * 事件发布器接口，抽象不同消息中间件的实现
 * 支持策略模式切换 Redis Stream / Kafka
 */
public interface EventPublisher {

    /**
     * 发布事件到消息中间件
     *
     * @param envelope   事件信封
     * @param targetName 目标名称 (Stream Key 或 Topic)
     * @return 消息ID
     */
    MessageId publish(EventEnvelope envelope, String targetName);

    /**
     * 消息ID封装，兼容不同中间件的消息标识
     */
    class MessageId implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String id;
        private final Object nativeId;

        public MessageId(String id, Object nativeId) {
            this.id = id;
            this.nativeId = nativeId;
        }

        public static MessageId of(String id) {
            return new MessageId(id, id);
        }

        public static MessageId of(String id, Object nativeId) {
            return new MessageId(id, nativeId);
        }

        public String getId() {
            return id;
        }

        @SuppressWarnings("unchecked")
        public <T> T getNativeId() {
            return (T) nativeId;
        }

        @Override
        public String toString() {
            return "MessageId{id='" + id + "'}";
        }
    }
}
