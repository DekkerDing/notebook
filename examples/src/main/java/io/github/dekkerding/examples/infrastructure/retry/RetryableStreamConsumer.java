package io.github.dekkerding.examples.infrastructure.retry;

import io.github.dekkerding.examples.domain.event.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 可重试的消费者装饰器，支持指数退避和死信队列
 */
@Slf4j
public class RetryableStreamConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final RetryPolicy retryPolicy;
    private final String dlqKeyPrefix;

    public RetryableStreamConsumer(RedisTemplate<String, String> redisTemplate,
                                   RetryPolicy retryPolicy,
                                   String dlqKeyPrefix) {
        this.redisTemplate = redisTemplate;
        this.retryPolicy = retryPolicy;
        this.dlqKeyPrefix = dlqKeyPrefix != null ? dlqKeyPrefix : "dlq:";
    }

    /**
     * 带重试的消息处理
     * @return true 处理成功, false 进入DLQ
     */
    public boolean handleWithRetry(MapRecord<String, String, String> record,
                                   Consumer<EventEnvelope> handler) {
        RecordId recordId = record.getId();
        String retryCountKey = "retry:" + recordId.getValue();

        int currentAttempt = getRetryCount(retryCountKey);

        try {
            EventEnvelope envelope = EventEnvelope.fromStreamRecord(record.getValue());
            handler.accept(envelope);
            cleanupRetryCount(retryCountKey);
            return true;
        } catch (Exception e) {
            currentAttempt++;
            if (currentAttempt >= retryPolicy.getMaxAttempts()) {
                log.error("💀 消息重试耗尽，进入死信队列: recordId={}, attempts={}",
                        recordId, currentAttempt, e);
                sendToDeadLetterQueue(record);
                cleanupRetryCount(retryCountKey);
                return false;
            }

            log.warn("🔄 消息处理失败，第 {}/{} 次重试: recordId={}",
                    currentAttempt, retryPolicy.getMaxAttempts(), recordId, e);
            incrementRetryCount(retryCountKey, currentAttempt);
            return false;
        }
    }

    private int getRetryCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private void incrementRetryCount(String key, int count) {
        redisTemplate.opsForValue().set(key, String.valueOf(count));
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    private void cleanupRetryCount(String key) {
        redisTemplate.delete(key);
    }

    private void sendToDeadLetterQueue(MapRecord<String, String, String> record) {
        String dlqKey = dlqKeyPrefix + record.getStream();
        Map<String, String> value = record.getValue();
        redisTemplate.opsForStream()
                .add(org.springframework.data.redis.connection.stream.StreamRecords
                        .newRecord()
                        .in(dlqKey)
                        .ofMap(value));
        log.error("💀 消息已写入死信队列: dlqKey={}, recordId={}", dlqKey, record.getId());
    }
}
