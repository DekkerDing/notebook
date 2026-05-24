package io.github.dekkerding.examples.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 两级缓存服务
 *
 * <p>基于Caffeine（本地）+ Redis（分布式）实现的两级缓存服务。
 *
 * <p>缓存策略：
 * <ul>
 *   <li>L1（本地缓存）：Caffeine，高频热数据，毫秒级访问</li>
 *   <li>L2（分布式缓存）：Redis，共享数据，跨节点访问</li>
 *   <li>自动同步：L1过期自动从L2加载</li>
 *   <li>主动失效：支持主动清除L1和L2缓存</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 获取缓存
 * User user = cacheService.get("user:123", User.class);
 *
 * // 带加载器的缓存获取
 * User user = cacheService.get("user:123", User.class, () -> {
 *     return userRepository.findById(123);
 * });
 *
 * // 设置缓存
 * cacheService.set("user:123", user, 30, TimeUnit.MINUTES);
 *
 * // 删除缓存
 * cacheService.delete("user:123");
 * }</pre>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class TwoLevelCacheService {

    @Autowired(required = false)
    private RedissonClient redisson;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_KEY_PREFIX = "cache:";

    /**
     * JSON序列化器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * L1本地缓存
     */
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>(1024);

    /**
     * L1缓存默认过期时间（毫秒）
     */
    private static final long L1_DEFAULT_TTL_MS = 60000; // 1分钟

    /**
     * L1缓存最大条目数
     */
    private static final int L1_MAX_SIZE = 10000;

    /**
     * L2缓存默认TTL（秒）
     */
    private static final long L2_DEFAULT_TTL_SEC = 1800; // 30分钟

    /**
     * 缓存统计
     */
    private final CacheStats stats = new CacheStats();

    /**
     * 定时清理L1过期缓存
     */
    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void init() {
        if (redisson == null) {
            log.warn("Redisson客户端未配置，L2缓存功能将不可用");
        }

        // 启动定时清理任务
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "cache-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                30, 30, TimeUnit.SECONDS
        );

        log.info("两级缓存服务初始化完成: L1 maxSize={}, L1 ttl={}ms, L2 ttl={}s",
                L1_MAX_SIZE, L1_DEFAULT_TTL_MS, L2_DEFAULT_TTL_SEC);
    }

    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("两级缓存服务已关闭: stats={}", stats);
    }

    // ==================== 基础缓存操作 ====================

    /**
     * 获取缓存（指定类型）
     *
     * @param key    缓存Key
     * @param type  目标类型
     * @param <T>    泛型类型
     * @return 缓存值，不存在返回null
     */
    public <T> T get(String key, Class<T> type) {
        String fullKey = buildFullKey(key);

        // 先从L1获取
        T value = getFromL1(fullKey, type);
        if (value != null) {
            stats.recordL1Hit();
            log.debug("L1缓存命中: key={}", fullKey);
            return value;
        }
        stats.recordL1Miss();

        // L1未命中，从L2获取
        value = getFromL2(fullKey, type);
        if (value != null) {
            stats.recordL2Hit();
            log.debug("L2缓存命中: key={}", fullKey);
            // 回写L1
            putToL1(fullKey, value, L1_DEFAULT_TTL_MS);
        } else {
            stats.recordL2Miss();
        }

        return value;
    }

    /**
     * 获取缓存（带加载器）
     *
     * <p>如果缓存不存在，通过loader加载并缓存。
     *
     * @param key     缓存Key
     * @param type    目标类型
     * @param loader  数据加载器
     * @param <T>     泛型类型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        T value = get(key, type);
        if (value != null) {
            return value;
        }

        // 加载数据
        value = loader.get();
        if (value != null) {
            set(key, value, L2_DEFAULT_TTL_SEC, TimeUnit.SECONDS);
        }

        return value;
    }

    /**
     * 设置缓存
     *
     * @param key        缓存Key
     * @param value      缓存值
     * @param ttl        TTL时长
     * @param timeUnit   时间单位
     * @param <T>        泛型类型
     */
    public <T> void set(String key, T value, long ttl, TimeUnit timeUnit) {
        String fullKey = buildFullKey(key);

        // 写入L1
        long ttlMs = timeUnit.toMillis(ttl);
        putToL1(fullKey, value, Math.min(ttlMs, L1_DEFAULT_TTL_MS));

        // 写入L2
        putToL2(fullKey, value, ttl, timeUnit);

        stats.recordPut();
        log.debug("缓存已设置: key={}, ttl={}{}", fullKey, ttl, timeUnit);
    }

    /**
     * 设置缓存（默认TTL）
     *
     * @param key    缓存Key
     * @param value  缓存值
     * @param <T>    泛型类型
     */
    public <T> void set(String key, T value) {
        set(key, value, L2_DEFAULT_TTL_SEC, TimeUnit.SECONDS);
    }

    /**
     * 删除缓存
     *
     * @param key  缓存Key
     */
    public void delete(String key) {
        String fullKey = buildFullKey(key);

        // 删除L1
        localCache.remove(fullKey);

        // 删除L2
        if (redisson != null) {
            redisson.getBucket(fullKey).delete();
        }

        stats.recordDelete();
        log.debug("缓存已删除: key={}", fullKey);
    }

    /**
     * 批量删除缓存
     *
     * @param keys  缓存Key集合
     */
    public void deleteBatch(Collection<String> keys) {
        keys.forEach(this::delete);
    }

    /**
     * 按前缀删除缓存
     *
     * @param prefix  Key前缀
     */
    public void deleteByPrefix(String prefix) {
        String fullPrefix = CACHE_KEY_PREFIX + prefix;

        // 清除L1缓存
        localCache.entrySet().removeIf(entry -> entry.getKey().startsWith(fullPrefix));

        // 清除L2缓存
        if (redisson != null) {
            RKeys keys = redisson.getKeys();
            Iterable<String> matchedKeys = keys.getKeysByPattern(fullPrefix + "*");
            for (String key : matchedKeys) {
                redisson.getBucket(key).delete();
            }
        }

        log.info("按前缀删除缓存: prefix={}", fullPrefix);
    }

    /**
     * 检查缓存是否存在
     *
     * @param key  缓存Key
     * @return 是否存在
     */
    public boolean exists(String key) {
        String fullKey = buildFullKey(key);

        // 检查L1
        CacheEntry entry = localCache.get(fullKey);
        if (entry != null && !entry.isExpired()) {
            return true;
        }

        // 检查L2
        if (redisson != null) {
            return redisson.getBucket(fullKey).isExists();
        }

        return false;
    }

    // ==================== L1本地缓存操作 ====================

    private <T> T getFromL1(String key, Class<T> type) {
        CacheEntry entry = localCache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                localCache.remove(key);
            }
            return null;
        }

        try {
            return objectMapper.readValue(entry.getValue(), type);
        } catch (Exception e) {
            log.error("L1缓存反序列化失败: key={}", key, e);
            localCache.remove(key);
            return null;
        }
    }

    private <T> void putToL1(String key, T value, long ttlMs) {
        try {
            // 检查缓存大小
            if (localCache.size() >= L1_MAX_SIZE) {
                // 简单淘汰策略：删除最早的一半条目
                localCache.entrySet().stream()
                        .limit(L1_MAX_SIZE / 2)
                        .forEach(entry -> localCache.remove(entry.getKey()));
            }

            String json = objectMapper.writeValueAsString(value);
            CacheEntry entry = new CacheEntry(json, System.currentTimeMillis() + ttlMs);
            localCache.put(key, entry);
        } catch (Exception e) {
            log.error("L1缓存序列化失败: key={}", key, e);
        }
    }

    // ==================== L2分布式缓存操作 ====================

    private <T> T getFromL2(String key, Class<T> type) {
        if (redisson == null) {
            return null;
        }

        try {
            RBucket<String> bucket = redisson.getBucket(key);
            String json = bucket.get();
            if (json == null) {
                return null;
            }

            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("L2缓存反序列化失败: key={}", key, e);
            return null;
        }
    }

    private <T> void putToL2(String key, T value, long ttl, TimeUnit timeUnit) {
        if (redisson == null) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(value);
            RBucket<String> bucket = redisson.getBucket(key);
            bucket.set(json, ttl, timeUnit);
        } catch (Exception e) {
            log.error("L2缓存序列化失败: key={}", key, e);
        }
    }

    // ==================== 缓存统计 ====================

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(stats);
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        stats.reset();
    }

    // ==================== 维护方法 ====================

    /**
     * 清理过期的L1缓存条目
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        int cleaned = 0;

        for (Map.Entry<String, CacheEntry> entry : localCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                localCache.remove(entry.getKey());
                cleaned++;
            }
        }

        if (cleaned > 0) {
            log.debug("清理过期L1缓存: count={}, remaining={}", cleaned, localCache.size());
        }
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        // 清空L1
        localCache.clear();

        // 清空L2
        if (redisson != null) {
            RKeys keys = redisson.getKeys();
            Iterable<String> cacheKeys = keys.getKeysByPattern(CACHE_KEY_PREFIX + "*");
            for (String key : cacheKeys) {
                redisson.getBucket(key).delete();
            }
        }

        log.info("所有缓存已清空");
    }

    /**
     * 获取L1缓存大小
     */
    public int getL1Size() {
        return localCache.size();
    }

    // ==================== 辅助方法 ====================

    private String buildFullKey(String key) {
        return CACHE_KEY_PREFIX + key;
    }

    // ==================== 内部类 ====================

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final String value;
        private final long expireTime;

        public CacheEntry(String value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public String getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 缓存统计
     */
    public static class CacheStats {
        private volatile long l1Hits = 0;
        private volatile long l1Misses = 0;
        private volatile long l2Hits = 0;
        private volatile long l2Misses = 0;
        private volatile long puts = 0;
        private volatile long deletes = 0;

        public CacheStats() {
        }

        public CacheStats(CacheStats other) {
            this.l1Hits = other.l1Hits;
            this.l1Misses = other.l1Misses;
            this.l2Hits = other.l2Hits;
            this.l2Misses = other.l2Misses;
            this.puts = other.puts;
            this.deletes = other.deletes;
        }

        public void recordL1Hit() {
            l1Hits++;
        }

        public void recordL1Miss() {
            l1Misses++;
        }

        public void recordL2Hit() {
            l2Hits++;
        }

        public void recordL2Miss() {
            l2Misses++;
        }

        public void recordPut() {
            puts++;
        }

        public void recordDelete() {
            deletes++;
        }

        public void reset() {
            l1Hits = 0;
            l1Misses = 0;
            l2Hits = 0;
            l2Misses = 0;
            puts = 0;
            deletes = 0;
        }

        public long getL1Hits() {
            return l1Hits;
        }

        public long getL1Misses() {
            return l1Misses;
        }

        public long getL2Hits() {
            return l2Hits;
        }

        public long getL2Misses() {
            return l2Misses;
        }

        public long getPuts() {
            return puts;
        }

        public long getDeletes() {
            return deletes;
        }

        public double getL1HitRate() {
            long total = l1Hits + l1Misses;
            return total == 0 ? 0 : (double) l1Hits / total;
        }

        public double getL2HitRate() {
            long total = l2Hits + l2Misses;
            return total == 0 ? 0 : (double) l2Hits / total;
        }

        public double getOverallHitRate() {
            long total = l1Hits + l1Misses;
            return total == 0 ? 0 : (double) l1Hits / total;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{l1Hits=%d, l1Misses=%d, l1HitRate=%.2f%%, l2Hits=%d, l2Misses=%d, l2HitRate=%.2f%%, puts=%d, deletes=%d}",
                    l1Hits, l1Misses, getL1HitRate() * 100,
                    l2Hits, l2Misses, getL2HitRate() * 100,
                    puts, deletes);
        }
    }
}
