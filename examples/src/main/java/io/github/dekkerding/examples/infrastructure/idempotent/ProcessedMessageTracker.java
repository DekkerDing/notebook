package io.github.dekkerding.examples.infrastructure.idempotent;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 已处理消息追踪器
 * 使用本地缓存 + Redis实现分布式幂等性检查
 */
@Slf4j
public class ProcessedMessageTracker {

    private final Cache<String, Boolean> localCache;
    private final RedisTemplate<String, String> redisTemplate;
    private final long keyTtlSeconds;
    private final boolean enabled;

    private static final String PROCESSED_KEY_PREFIX = "examplesApplication:processed:";

    private static final RedisScript<Boolean> ADD_PROCESSED_SCRIPT = RedisScript.of(
            "redis.call('SADD', KEYS[1], ARGV[1]) " +
                    "redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                    "return 1",
            Boolean.class
    );

    private static final RedisScript<Boolean> CHECK_PROCESSED_SCRIPT = RedisScript.of(
            "return redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1",
            Boolean.class
    );

    public ProcessedMessageTracker(
            long keyTtlSeconds,
            boolean enabled,
            RedisTemplate<String, String> redisTemplate) {
        this.keyTtlSeconds = keyTtlSeconds;
        this.enabled = enabled;
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    /**
     * 检查消息是否已处理
     *
     * @param messageId     消息ID
     * @param consumerGroup 消费者组
     * @param streamKey     Stream Key
     * @return true表示已处理，false表示未处理
     */
    public boolean isProcessed(String messageId, String consumerGroup, String streamKey) {
        if (!enabled) {
            return false;
        }

        String cacheKey = buildCacheKey(messageId, consumerGroup, streamKey);

        Boolean cached = localCache.getIfPresent(cacheKey);
        if (Boolean.TRUE.equals(cached)) {
            log.trace("本地缓存: 消息已处理 {}", messageId);
            return true;
        }

        String redisKey = buildRedisKey(consumerGroup, streamKey);
        Boolean processed = redisTemplate.execute(
                CHECK_PROCESSED_SCRIPT,
                Collections.singletonList(redisKey),
                messageId
        );

        if (Boolean.TRUE.equals(processed)) {
            localCache.put(cacheKey, true);
            log.debug("Redis检查: 消息已处理 {}", messageId);
            return true;
        }

        return false;
    }

    /**
     * 标记消息为已处理
     *
     * @param messageId     消息ID
     * @param consumerGroup 消费者组
     * @param streamKey     Stream Key
     */
    public void markAsProcessed(String messageId, String consumerGroup, String streamKey) {
        if (!enabled) {
            return;
        }

        String cacheKey = buildCacheKey(messageId, consumerGroup, streamKey);
        String redisKey = buildRedisKey(consumerGroup, streamKey);

        localCache.put(cacheKey, true);

        redisTemplate.execute(
                ADD_PROCESSED_SCRIPT,
                Collections.singletonList(redisKey),
                messageId,
                String.valueOf(keyTtlSeconds)
        );

        log.trace("标记消息已处理: messageId={}, group={}, stream={}",
                messageId, consumerGroup, streamKey);
    }

    /**
     * 批量检查消息是否已处理
     */
    public boolean[] checkBatch(String[] messageIds, String consumerGroup, String streamKey) {
        boolean[] results = new boolean[messageIds.length];
        for (int i = 0; i < messageIds.length; i++) {
            results[i] = isProcessed(messageIds[i], consumerGroup, streamKey);
        }
        return results;
    }

    /**
     * 清理指定Stream的已处理记录
     */
    public void cleanup(String consumerGroup, String streamKey) {
        String redisKey = buildRedisKey(consumerGroup, streamKey);
        redisTemplate.delete(redisKey);
        log.info("清理已处理记录: {}", redisKey);
    }

    /**
     * 获取已处理消息数量
     */
    public long getProcessedCount(String consumerGroup, String streamKey) {
        String redisKey = buildRedisKey(consumerGroup, streamKey);
        Long size = redisTemplate.opsForSet().size(redisKey);
        return size != null ? size : 0;
    }

    private String buildCacheKey(String messageId, String consumerGroup, String streamKey) {
        return messageId + ":" + consumerGroup + ":" + streamKey;
    }

    private String buildRedisKey(String consumerGroup, String streamKey) {
        return PROCESSED_KEY_PREFIX + consumerGroup + ":" + streamKey;
    }
}
