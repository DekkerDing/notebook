package io.github.dekkerding.examples.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Stream 事件桥接配置属性
 * 增强版：支持分区、幂等性、负载均衡等Kafka功能
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.redis.stream.event")
public class RedisStreamEventProperties {

    private boolean enabled = true;

    private String keyPrefix = "stream:";

    private String defaultGroup = "default-group";

    private String consumerName = "consumer";

    private long pollTimeout = 2000;

    private int batchSize = 10;

    private PartitionProperties partition = new PartitionProperties();

    private IdempotentProperties idempotent = new IdempotentProperties();

    private ConsumerGroupProperties consumerGroup = new ConsumerGroupProperties();

    private RewindProperties rewind = new RewindProperties();

    private RetryProperties retry = new RetryProperties();

    private DlqProperties dlq = new DlqProperties();

    private Map<String, String> routes = new HashMap<>();

    @Getter
    @Setter
    public static class PartitionProperties {
        private boolean enabled = false;
        private int count = 3;
        private String strategy = "hash"; // round_robin, hash, custom
    }

    @Getter
    @Setter
    public static class IdempotentProperties {
        private boolean enabled = true;
        private long keyTtl = 86400; // 24小时
    }

    @Getter
    @Setter
    public static class ConsumerGroupProperties {
        private boolean autoBalance = true;
        private long rebalanceTimeout = 60; // 秒
        private int heartbeatInterval = 10; // 秒
    }

    @Getter
    @Setter
    public static class RewindProperties {
        private boolean enabled = true;
        private int maxHistoryDays = 7;
    }

    @Getter
    @Setter
    public static class RetryProperties {
        private int maxAttempts = 3;
        private long initialInterval = 1000;
        private double backoffMultiplier = 2.0;
    }

    @Getter
    @Setter
    public static class DlqProperties {
        private boolean enabled = true;
        private String keyPrefix = "dlq:";
    }
}
