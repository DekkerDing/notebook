package io.github.dekkerding.examples.infrastructure.partition;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分区管理器
 * 管理Stream的分区创建和命名
 */
@Slf4j
public class PartitionManager {

    private final String keyPrefix;
    private final int partitionCount;
    private final PartitionStrategy partitionStrategy;
    private final RedisTemplate<String, String> redisTemplate;

    private final Map<String, List<String>> partitionCache = new ConcurrentHashMap<>();

    public PartitionManager(
            String keyPrefix,
            int partitionCount,
            PartitionStrategy partitionStrategy,
            RedisTemplate<String, String> redisTemplate) {
        this.keyPrefix = keyPrefix;
        this.partitionCount = partitionCount;
        this.partitionStrategy = partitionStrategy;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 为事件类型创建分区Stream（如果不存在）
     */
    public void ensurePartitions(String eventType) {
        String cacheKey = eventType;
        if (partitionCache.containsKey(cacheKey)) {
            return;
        }

        for (int i = 0; i < partitionCount; i++) {
            String streamKey = getPartitionStreamKey(eventType, i);
            try {
                redisTemplate.opsForStream().createGroup(streamKey, "default-group");
                log.info("创建分区Stream: {}", streamKey);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                    log.debug("消费者组已存在: {}", streamKey);
                } else {
                    log.warn("创建Stream失败: {}", streamKey, e);
                }
            }
        }

        // 动态生成分区列表，使用实际配置的分区数量
        List<String> partitions = new java.util.ArrayList<>(partitionCount);
        for (int i = 0; i < partitionCount; i++) {
            partitions.add(eventType + ":" + i);
        }
        partitionCache.put(cacheKey, partitions);
    }

    /**
     * 获取事件类型对应的所有分区Stream Key
     */
    public List<String> getPartitionStreamKeys(String eventType) {
        String[] keys = new String[partitionCount];
        for (int i = 0; i < partitionCount; i++) {
            keys[i] = getPartitionStreamKey(eventType, i);
        }
        return Arrays.asList(keys);
    }

    /**
     * 根据事件Key计算目标分区
     */
    public int getPartition(String eventKey, String eventType) {
        return partitionStrategy.partition(eventKey, partitionCount);
    }

    /**
     * 获取指定分区的Stream Key
     */
    public String getPartitionStreamKey(String eventType, int partition) {
        if (partition < 0 || partition >= partitionCount) {
            throw new IllegalArgumentException(
                    "分区号无效: " + partition + ", 范围: [0, " + (partitionCount - 1) + "]");
        }
        return keyPrefix + eventType + ":" + partition;
    }

    /**
     * 获取分区数量
     */
    public int getPartitionCount() {
        return partitionCount;
    }

    /**
     * 获取当前使用的分区策略
     */
    public PartitionStrategy getPartitionStrategy() {
        return partitionStrategy;
    }

    /**
     * 清空分区缓存
     */
    public void clearCache() {
        partitionCache.clear();
    }
}
