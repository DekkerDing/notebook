package io.github.dekkerding.examples.infrastructure.consumer;

import io.github.dekkerding.examples.application.EventConsumingService;
import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.domain.model.ConsumerInfo;
import io.github.dekkerding.examples.infrastructure.retry.RetryableStreamConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Stream 事件消费者，管理订阅生命周期
 */
@Slf4j
public class RedisStreamEventConsumer {

    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final RedisTemplate<String, String> redisTemplate;
    private final EventConsumingService consumingService;
    private final RetryableStreamConsumer retryableConsumer;
    private final Map<String, Subscription> subscriptionHolder = new ConcurrentHashMap<>();
    private final Map<String, ConsumerInfo> consumerInfoMap = new ConcurrentHashMap<>();

    public RedisStreamEventConsumer(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            RedisTemplate<String, String> redisTemplate,
            EventConsumingService consumingService,
            RetryableStreamConsumer retryableConsumer) {
        this.container = container;
        this.redisTemplate = redisTemplate;
        this.consumingService = consumingService;
        this.retryableConsumer = retryableConsumer;
    }

    public void start(ConsumerInfo consumerInfo) {
        String subscriptionId = consumerInfo.toSubscriptionId();

        if (subscriptionHolder.containsKey(subscriptionId)) {
            log.warn("消费者已在运行: {}", subscriptionId);
            return;
        }

        try {
            ensureConsumerGroup(consumerInfo.getStreamKey(), consumerInfo.getGroupName());

            StreamOffset<String> offset = StreamOffset.create(
                    consumerInfo.getStreamKey(), ReadOffset.lastConsumed());

            Consumer consumer = Consumer.from(
                    consumerInfo.getGroupName(), consumerInfo.getConsumerName());

            Subscription subscription = container.receive(
                    consumer, offset,
                    record -> handleMessage(record, consumerInfo.getGroupName()));

            subscriptionHolder.put(subscriptionId, subscription);
            consumerInfoMap.put(subscriptionId, consumerInfo);

            log.info("✅ 事件消费者已启动: {}", subscriptionId);
        } catch (Exception e) {
            log.error("❌ 启动事件消费者失败: {}", subscriptionId, e);
        }
    }

    public void stop(ConsumerInfo consumerInfo) {
        String subscriptionId = consumerInfo.toSubscriptionId();
        Subscription subscription = subscriptionHolder.remove(subscriptionId);
        consumerInfoMap.remove(subscriptionId);

        if (subscription != null) {
            subscription.cancel();
            log.info("🛑 事件消费者已停止: {}", subscriptionId);
        } else {
            log.warn("消费者不存在: {}", subscriptionId);
        }
    }

    public boolean isActive(ConsumerInfo consumerInfo) {
        return subscriptionHolder.containsKey(consumerInfo.toSubscriptionId());
    }

    public List<String> getActiveSubscriptions() {
        return new ArrayList<>(subscriptionHolder.keySet());
    }

    public Map<String, ConsumerInfo> getConsumerInfoMap() {
        return new ConcurrentHashMap<>(consumerInfoMap);
    }

    private void handleMessage(MapRecord<String, String, String> record, String groupName) {
        log.info("📨 收到事件消息 - Stream: {}, ID: {}", record.getStream(), record.getId());

        boolean success = retryableConsumer.handleWithRetry(record, envelope -> {
            consumingService.dispatch(envelope);
        });

        if (success) {
            redisTemplate.opsForStream().acknowledge(
                    record.getStream(), groupName, record.getId());
            log.debug("✅ 事件消息已确认: {}", record.getId());
        }
    }

    private void ensureConsumerGroup(String streamKey, String groupName) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), groupName);
            log.info("创建消费者组: {} -> {}", streamKey, groupName);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("消费者组已存在: {} -> {}", streamKey, groupName);
            } else {
                throw e;
            }
        }
    }
}
