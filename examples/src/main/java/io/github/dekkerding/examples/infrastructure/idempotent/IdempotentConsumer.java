package io.github.dekkerding.examples.infrastructure.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;

import java.util.function.Consumer;

/**
 * 幂等性消费者包装器
 * 确保消息只被处理一次
 */
@Slf4j
public class IdempotentConsumer {

    private final ProcessedMessageTracker tracker;
    private final boolean enabled;

    public IdempotentConsumer(ProcessedMessageTracker tracker, boolean enabled) {
        this.tracker = tracker;
        this.enabled = enabled;
    }

    /**
     * 幂等地消费消息
     * 如果消息已处理则跳过
     *
     * @param record        Redis Stream消息记录
     * @param consumerGroup 消费者组
     * @param handler       消息处理逻辑
     * @return true表示处理成功，false表示跳过或失败
     */
    public boolean consume(
            MapRecord<String, String, String> record,
            String consumerGroup,
            Consumer<MapRecord<String, String, String>> handler) {

        String messageId = record.getId().getValue();
        String streamKey = record.getStream();

        if (enabled && tracker.isProcessed(messageId, consumerGroup, streamKey)) {
            log.debug("⏭️ 消息已处理，跳过: messageId={}, stream={}", messageId, streamKey);
            return true;
        }

        try {
            handler.accept(record);

            if (enabled) {
                tracker.markAsProcessed(messageId, consumerGroup, streamKey);
            }

            log.debug("✅ 消息处理成功: messageId={}, stream={}", messageId, streamKey);
            return true;

        } catch (Exception e) {
            log.error("❌ 消息处理失败: messageId={}, stream={}", messageId, streamKey, e);
            return false;
        }
    }

    /**
     * 获取已处理消息追踪器
     */
    public ProcessedMessageTracker getTracker() {
        return tracker;
    }
}
