package io.github.dekkerding.examples.infrastructure.health;

import io.github.dekkerding.examples.infrastructure.consumer.RedisStreamEventConsumer;
import io.github.dekkerding.examples.infrastructure.idempotent.ProcessedMessageTracker;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream 消费者健康检查指示器（增强版）
 * 支持分区监控、幂等性统计
 */
public class StreamConsumerHealthIndicator implements HealthIndicator {

    private final RedisStreamEventConsumer eventConsumer;
    private final ProcessedMessageTracker messageTracker;

    public StreamConsumerHealthIndicator(RedisStreamEventConsumer eventConsumer) {
        this(eventConsumer, null);
    }

    public StreamConsumerHealthIndicator(
            RedisStreamEventConsumer eventConsumer,
            ProcessedMessageTracker messageTracker) {
        this.eventConsumer = eventConsumer;
        this.messageTracker = messageTracker;
    }

    @Override
    public Health health() {
        List<String> activeSubscriptions = eventConsumer.getActiveSubscriptions();
        Map<String, Object> details = new HashMap<>();

        details.put("activeConsumers", activeSubscriptions.size());
        details.put("subscriptions", activeSubscriptions);

        if (messageTracker != null) {
            Map<String, Long> processedCounts = getProcessedCounts(activeSubscriptions);
            details.put("processedMessageCounts", processedCounts);
        }

        if (activeSubscriptions.isEmpty()) {
            return Health.unknown()
                    .withDetails(details)
                    .withDetail("message", "无活跃消费者")
                    .build();
        }

        return Health.up()
                .withDetails(details)
                .build();
    }

    private Map<String, Long> getProcessedCounts(List<String> subscriptions) {
        Map<String, Long> counts = new HashMap<>();
        if (messageTracker == null) {
            return counts;
        }

        for (String subscription : subscriptions) {
            try {
                String[] parts = subscription.split(":");
                if (parts.length >= 3) {
                    String streamKey = String.join(":", parts[0], parts[1], parts[2]);
                    long count = messageTracker.getProcessedCount(parts[1], streamKey);
                    counts.put(subscription, count);
                }
            } catch (Exception e) {
                counts.put(subscription, -1L);
            }
        }
        return counts;
    }
}

