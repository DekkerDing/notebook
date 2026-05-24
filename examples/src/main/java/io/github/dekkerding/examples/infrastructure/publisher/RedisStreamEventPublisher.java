package io.github.dekkerding.examples.infrastructure.publisher;

import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.infrastructure.publisher.EventPublisher.MessageId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * Redis Stream 事件发布器，将 EventEnvelope 写入指定的 Stream
 * 实现 EventPublisher 接口以支持策略模式切换
 */
@Slf4j
public class RedisStreamEventPublisher implements EventPublisher {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisStreamEventPublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RecordId publishToStream(EventEnvelope envelope, String streamKey) {
        MessageId messageId = publish(envelope, streamKey);
        return RecordId.of(messageId.getId());
    }

    @Override
    public MessageId publish(EventEnvelope envelope, String streamKey) {
        Map<String, String> record = envelope.toStreamRecord();

        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .in(streamKey)
                        .ofMap(record));

        log.info("📤 事件已发布到 Stream - key: {}, recordId: {}, eventType: {}",
                streamKey, recordId, envelope.getMetadata().getEventType());

        return MessageId.of(recordId.getValue(), recordId);
    }
}
