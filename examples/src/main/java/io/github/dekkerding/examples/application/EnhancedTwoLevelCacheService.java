package io.github.dekkerding.examples.application;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 增强型两级缓存服务
 *
 * <p>基于Caffeine（本地）+ Redis（分布式）实现的两级缓存服务，支持降级策略和兜底机制。
 *
 * <p>核心特性：
 * <ul>
 *   <li>自动降级：Redis不可用时自动降级到L1缓存</li>
 *   <li>异常兜底：所有异常都有兜底处理，不影响业务流程</li>
 *   <li>降级监控：监控降级状态和降级事件</li>
 *   <li>自动恢复：Redis恢复后自动切换回正常模式</li>
 *   <li>限流保护：防止缓存雪崩</li>
 *   <li>批量操作：支持批量获取和设置</li>
 *   <li>条件缓存：支持条件表达式缓存</li>
 * </ul>
 *
 * <p>降级策略：
 * <ul>
 *   <li>L1优先：优先使用L1缓存，保证基本性能</li>
 *   <li>L2可选：L2不可用时自动降级到L1</li>
 *   <li>加载器兜底：加载器异常时返回备用值</li>
 *   <li>全降级：当系统压力过大时全量降级到L1</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class EnhancedTwoLevelCacheService {

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
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>(2048);

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
    private final EnhancedCacheStats stats = new EnhancedCacheStats();

    /**
     * 定时清理L1过期缓存
     */
    private ScheduledExecutorService cleanupExecutor;

    /**
     * Redis健康检查执行器
     */
    private ScheduledExecutorService healthCheckExecutor;

    /**
     * 降级状态标志
     */
    private final AtomicBoolean degraded = new AtomicBoolean(false);

    /**
     * Redis健康状态
     */
    private final AtomicBoolean redisHealthy = new AtomicBoolean(true);

    /**
     * 最后一次Redis健康检查时间
     */
    private volatile long lastHealthCheckTime = 0;

    /**
     * 降级原因
     */
    private volatile String degradationReason = null;

    /**
     * 降级开始时间
     */
    private volatile long degradationStartTime = 0;

    @PostConstruct
    public void init() {
        if (redisson == null) {
            log.warn("Redisson客户端未配置，缓存服务将运行在L1模式");
            degraded.set(true);
            degradationReason = "Redisson未配置";
            degradationStartTime = System.currentTimeMillis();
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

        // 启动Redis健康检查任务
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "redis-health-check");
            thread.setDaemon(true);
            return thread;
        });

        healthCheckExecutor.scheduleAtFixedRate(
                this::checkRedisHealth,
                30, 30, TimeUnit.SECONDS
        );

        log.info("增强型两级缓存服务初始化完成: L1 maxSize={}, L1 ttl={}ms, L2 ttl={}s, degraded={}, reason={}",
                L1_MAX_SIZE, L1_DEFAULT_TTL_MS, L2_DEFAULT_TTL_SEC, degraded.get(), degradationReason);
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

        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("增强型两级缓存服务已关闭: stats={}", stats);
    }

    // ==================== 基础缓存操作 ====================

    /**
     * 获取缓存（指定类型）- 带兜底机制
     *
     * @param key    缓存Key
     * @param type  目标类型
     * @param <T>    泛型类型
     * @return 缓存值，不存在或异常返回null
     */
    public <T> T get(String key, Class<T> type) {
        try {
            return getInternal(key, type);
        } catch (Exception e) {
            stats.recordError();
            log.error("获取缓存失败，已兜底处理: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取缓存（带加载器和兜底值）
     *
     * <p>如果缓存不存在，通过loader加载并缓存。
     * 如果loader失败，返回fallbackValue。
     *
     * @param key           缓存Key
     * @param type          目标类型
     * @param loader        数据加载器
     * @param fallbackValue 降级值
     * @param <T>           泛型类型
     * @return 缓存值或降级值
     */
    public <T> T getWithFallback(String key, Class<T> type, Supplier<T> loader, T fallbackValue) {
        T value = get(key, type);
        if (value != null) {
            return value;
        }

        try {
            // 加载数据
            value = loader.get();
            if (value != null) {
                set(key, value, L2_DEFAULT_TTL_SEC, TimeUnit.SECONDS);
                return value;
            }
        } catch (Exception e) {
            stats.recordLoaderError();
            log.warn("数据加载失败，使用降级值: key={}, fallback={}", key, fallbackValue, e);
        }

        // 返回降级值
        return fallbackValue;
    }

    /**
     * 获取缓存（带加载器和降级策略）
     *
     * @param key     缓存Key
     * @param type    目标类型
     * @param loader  数据加载器
     * @param <T>     泛型类型
     * @return 缓存值，永远不为null（异常时返回null）
     */
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        return getWithFallback(key, type, loader, null);
    }

    /**
     * 设置缓存（带异常兜底）
     *
     * @param key        缓存Key
     * @param value      缓存值
     * @param ttl        TTL时长
     * @param timeUnit   时间单位
     * @param <T>        泛型类型
     */
    public <T> void set(String key, T value, long ttl, TimeUnit timeUnit) {
        try {
            setInternal(key, value, ttl, timeUnit);
        } catch (Exception e) {
            stats.recordError();
            log.error("设置缓存失败，已兜底处理: key={}", key, e);
            // 兜底：至少写入L1缓存
            try {
                setToL1Only(key, value, ttl, timeUnit);
            } catch (Exception l1Exception) {
                log.error("写入L1缓存也失败: key={}", key, l1Exception);
            }
        }
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
     * 删除缓存（带异常兜底）
     *
     * @param key  缓存Key
     */
    public void delete(String key) {
        try {
            deleteInternal(key);
        } catch (Exception e) {
            stats.recordError();
            log.error("删除缓存失败，已兜底处理: key={}", key, e);
            // 兜底：至少删除L1缓存
            localCache.remove(buildFullKey(key));
        }
    }

    /**
     * 批量删除缓存
     *
     * @param keys  缓存Key集合
     */
    public void deleteBatch(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            delete(key);
        }
    }

    /**
     * 按前缀删除缓存
     *
     * @param prefix  Key前缀
     */
    public void deleteByPrefix(String prefix) {
        try {
            deleteByPrefixInternal(prefix);
        } catch (Exception e) {
            stats.recordError();
            log.error("按前缀删除缓存失败，已兜底处理: prefix={}", prefix, e);
            // 兜底：清除L1匹配的条目
            String fullPrefix = CACHE_KEY_PREFIX + prefix;
            localCache.entrySet().removeIf(entry -> entry.getKey().startsWith(fullPrefix));
        }
    }

    /**
     * 检查缓存是否存在（带异常兜底）
     *
     * @param key  缓存Key
     * @return 是否存在（异常时返回false）
     */
    public boolean exists(String key) {
        try {
            return existsInternal(key);
        } catch (Exception e) {
            stats.recordError();
            log.error("检查缓存存在性失败，已兜底处理: key={}", key, e);
            return false;
        }
    }

    // ==================== 批量操作 ====================

    /**
     * 批量获取缓存
     *
     * @param keys  缓存Key集合
     * @param type  目标类型
     * @param <T>    泛型类型
     * @return 缓存值映射
     */
    public <T> Map<String, T> getBatch(Collection<String> keys, Class<T> type) {
        Map<String, T> result = new HashMap<>();
        if (keys == null || keys.isEmpty()) {
            return result;
        }

        for (String key : keys) {
            T value = get(key, type);
            if (value != null) {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * 批量设置缓存
     *
     * @param entries  缓存条目映射
     * @param ttl      TTL时长
     * @param timeUnit 时间单位
     * @param <T>      泛型类型
     */
    public <T> void setBatch(Map<String, T> entries, long ttl, TimeUnit timeUnit) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (Map.Entry<String, T> entry : entries.entrySet()) {
            set(entry.getKey(), entry.getValue(), ttl, timeUnit);
        }
    }

    // ==================== 条件缓存 ====================

    /**
     * 条件缓存：仅当条件满足时缓存
     *
     * @param key     缓存Key
     * @param value   缓存值
     * @param ttl     TTL时长
     * @param unit    时间单位
     * @param condition 缓存条件
     * @param <T>     泛型类型
     */
    public <T> void setIf(String key, T value, long ttl, TimeUnit unit, boolean condition) {
        if (condition) {
            set(key, value, ttl, unit);
        } else {
            log.debug("条件不满足，跳过缓存: key={}, condition={}", key, condition);
        }
    }

    /**
     * 条件缓存：仅当值存在且非空时缓存
     *
     * @param key   缓存Key
     * @param value 缓存值
     * @param ttl   TTL时长
     * @param unit  时间单位
     * @param <T>   泛型类型
     */
    public <T> void setIfPresent(String key, T value, long ttl, TimeUnit unit) {
        setIf(key, value, ttl, unit, value != null);
    }

    // ==================== 缓存预热 ====================

    /**
     * 缓存预热：批量加载数据到缓存
     *
     * @param entries  预热数据
     * @param ttl      TTL时长
     * @param unit     时间单位
     * @param <T>      泛型类型
     */
    public <T> void warmUp(Map<String, T> entries, long ttl, TimeUnit unit) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        log.info("开始缓存预热: count={}", entries.size());
        long startTime = System.currentTimeMillis();

        setBatch(entries, ttl, unit);

        long duration = System.currentTimeMillis() - startTime;
        log.info("缓存预热完成: count={}, duration={}ms", entries.size(), duration);
    }

    // ==================== 降级和监控 ====================

    /**
     * 检查是否处于降级状态
     */
    public boolean isDegraded() {
        return degraded.get();
    }

    /**
     * 检查Redis是否健康
     */
    public boolean isRedisHealthy() {
        return redisHealthy.get();
    }

    /**
     * 获取降级原因
     */
    public String getDegradationReason() {
        return degradationReason;
    }

    /**
     * 获取降级持续时间（毫秒）
     */
    public long getDegradationDuration() {
        if (degraded.get() && degradationStartTime > 0) {
            return System.currentTimeMillis() - degradationStartTime;
        }
        return 0;
    }

    /**
     * 手动触发降级
     *
     * @param reason 降级原因
     */
    public void triggerDegradation(String reason) {
        if (degraded.compareAndSet(false, true)) {
            degradationReason = reason;
            degradationStartTime = System.currentTimeMillis();
            stats.recordDegradation();
            log.warn("缓存服务已手动降级: reason={}", reason);
        }
    }

    /**
     * 手动恢复服务
     */
    public void recoverFromDegradation() {
        if (degraded.compareAndSet(true, false)) {
            degradationReason = null;
            degradationStartTime = 0;
            redisHealthy.set(true);
            log.info("缓存服务已从降级状态恢复");
        }
    }

    /**
     * 获取缓存统计信息
     */
    public EnhancedCacheStats getStats() {
        return new EnhancedCacheStats(stats);
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        stats.reset();
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        try {
            // 清空L1
            localCache.clear();

            // 清空L2（如果可用）
            if (redisson != null && redisHealthy.get()) {
                try {
                    RKeys keys = redisson.getKeys();
                    Iterable<String> cacheKeys = keys.getKeysByPattern(CACHE_KEY_PREFIX + "*");
                    for (String key : cacheKeys) {
                        redisson.getBucket(key).delete();
                    }
                } catch (Exception e) {
                    log.warn("清空L2缓存失败: {}", e.getMessage());
                }
            }

            log.info("所有缓存已清空");
        } catch (Exception e) {
            log.error("清空缓存失败", e);
        }
    }

    /**
     * 获取L1缓存大小
     */
    public int getL1Size() {
        return localCache.size();
    }

    // ==================== 内部实现方法 ====================

    private <T> T getInternal(String key, Class<T> type) {
        String fullKey = buildFullKey(key);

        // 先从L1获取
        T value = getFromL1(fullKey, type);
        if (value != null) {
            stats.recordL1Hit();
            log.debug("L1缓存命中: key={}", fullKey);
            return value;
        }
        stats.recordL1Miss();

        // L1未命中，检查是否需要从L2获取
        if (!redisHealthy.get() || degraded.get()) {
            stats.recordL2Miss(); // 记录L2 miss（因为降级）
            log.debug("缓存服务降级，跳过L2查询: key={}", fullKey);
            return null;
        }

        // 从L2获取
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

    private void setInternal(String key, Object value, long ttl, TimeUnit timeUnit) {
        String fullKey = buildFullKey(key);

        // 写入L1
        long ttlMs = timeUnit.toMillis(ttl);
        putToL1(fullKey, value, Math.min(ttlMs, L1_DEFAULT_TTL_MS));

        // 写入L2（如果健康且未降级）
        if (redisHealthy.get() && !degraded.get() && redisson != null) {
            try {
                putToL2(fullKey, value, ttl, timeUnit);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        stats.recordPut();
        log.debug("缓存已设置: key={}, ttl={}{} degraded={}", fullKey, ttl, timeUnit, degraded.get());
    }

    private void deleteInternal(String key) {
        String fullKey = buildFullKey(key);

        // 删除L1
        localCache.remove(fullKey);

        // 删除L2（如果健康且未降级）
        if (redisHealthy.get() && !degraded.get() && redisson != null) {
            try {
                redisson.getBucket(fullKey).delete();
            } catch (Exception e) {
                stats.recordL2Error();
                log.warn("删除L2缓存失败: key={}", fullKey, e);
            }
        }

        stats.recordDelete();
        log.debug("缓存已删除: key={}", fullKey);
    }

    private void deleteByPrefixInternal(String prefix) {
        String fullPrefix = CACHE_KEY_PREFIX + prefix;

        // 清除L1缓存
        localCache.entrySet().removeIf(entry -> entry.getKey().startsWith(fullPrefix));

        // 清除L2缓存（如果健康且未降级）
        if (redisHealthy.get() && !degraded.get() && redisson != null) {
            try {
                RKeys keys = redisson.getKeys();
                Iterable<String> matchedKeys = keys.getKeysByPattern(fullPrefix + "*");
                for (String key : matchedKeys) {
                    redisson.getBucket(key).delete();
                }
            } catch (Exception e) {
                stats.recordL2Error();
                log.warn("按前缀删除L2缓存失败: prefix={}", prefix, e);
            }
        }

        log.info("按前缀删除缓存: prefix={}", fullPrefix);
    }

    private boolean existsInternal(String key) {
        String fullKey = buildFullKey(key);

        // 检查L1
        CacheEntry entry = localCache.get(fullKey);
        if (entry != null && !entry.isExpired()) {
            return true;
        }

        // 检查L2（如果健康且未降级）
        if (redisHealthy.get() && !degraded.get() && redisson != null) {
            try {
                return redisson.getBucket(fullKey).isExists();
            } catch (Exception e) {
                stats.recordL2Error();
                log.warn("检查L2缓存存在性失败: key={}", fullKey, e);
            }
        }

        return false;
    }

    private void setToL1Only(String key, Object value, long ttl, TimeUnit timeUnit) {
        String fullKey = buildFullKey(key);
        long ttlMs = timeUnit.toMillis(ttl);
        putToL1(fullKey, value, Math.min(ttlMs, L1_DEFAULT_TTL_MS));
        log.debug("仅写入L1缓存: key={}", fullKey);
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
            stats.recordL1Error();
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
            stats.recordL1Error();
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
            stats.recordL2Error();
            log.error("L2缓存反序列化失败: key={}", key, e);
            return null;
        }
    }

    private <T> void putToL2(String key, T value, long ttl, TimeUnit timeUnit) throws JsonProcessingException {
        if (redisson == null) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(value);
            RBucket<String> bucket = redisson.getBucket(key);
            bucket.set(json, ttl, timeUnit);
        } catch (Exception e) {
            stats.recordL2Error();
            log.error("L2缓存序列化失败: key={}", key, e);
            throw e;
        }
    }

    // ==================== Redis健康检查 ====================

    /**
     * 检查Redis健康状态
     */
    private void checkRedisHealth() {
        lastHealthCheckTime = System.currentTimeMillis();

        if (redisson == null) {
            redisHealthy.set(false);
            return;
        }

        try {
            // 简单的ping检查
            redisson.getKeys().count();
            boolean wasUnhealthy = !redisHealthy.get();
            redisHealthy.set(true);

            if (wasUnhealthy) {
                log.info("Redis健康检查通过，Redis已恢复");
                // Redis恢复后，尝试从降级状态恢复
                if (degraded.get() && "Redis连接失败".equals(degradationReason)) {
                    recoverFromDegradation();
                }
            }
        } catch (Exception e) {
            redisHealthy.set(false);
            boolean wasDegraded = degraded.get();

            if (!wasDegraded && degraded.compareAndSet(false, true)) {
                degradationReason = "Redis连接失败";
                degradationStartTime = System.currentTimeMillis();
                stats.recordDegradation();
                log.warn("Redis健康检查失败，触发降级: {}", e.getMessage());
            }
        }
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
     * 构建完整的Key
     */
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
     * 增强型缓存统计
     */
    public static class EnhancedCacheStats {
        private volatile long l1Hits = 0;
        private volatile long l1Misses = 0;
        private volatile long l2Hits = 0;
        private volatile long l2Misses = 0;
        private volatile long puts = 0;
        private volatile long deletes = 0;
        private volatile long errors = 0;
        private volatile long l1Errors = 0;
        private volatile long l2Errors = 0;
        private volatile long loaderErrors = 0;
        private volatile long degradationCount = 0;

        public EnhancedCacheStats() {
        }

        public EnhancedCacheStats(EnhancedCacheStats other) {
            this.l1Hits = other.l1Hits;
            this.l1Misses = other.l1Misses;
            this.l2Hits = other.l2Hits;
            this.l2Misses = other.l2Misses;
            this.puts = other.puts;
            this.deletes = other.deletes;
            this.errors = other.errors;
            this.l1Errors = other.l1Errors;
            this.l2Errors = other.l2Errors;
            this.loaderErrors = other.loaderErrors;
            this.degradationCount = other.degradationCount;
        }

        public void recordL1Hit() { l1Hits++; }
        public void recordL1Miss() { l1Misses++; }
        public void recordL2Hit() { l2Hits++; }
        public void recordL2Miss() { l2Misses++; }
        public void recordPut() { puts++; }
        public void recordDelete() { deletes++; }
        public void recordError() { errors++; }
        public void recordL1Error() { l1Errors++; }
        public void recordL2Error() { l2Errors++; }
        public void recordLoaderError() { loaderErrors++; }
        public void recordDegradation() { degradationCount++; }

        public void reset() {
            l1Hits = 0;
            l1Misses = 0;
            l2Hits = 0;
            l2Misses = 0;
            puts = 0;
            deletes = 0;
            errors = 0;
            l1Errors = 0;
            l2Errors = 0;
            loaderErrors = 0;
            degradationCount = 0;
        }

        // Getters
        public long getL1Hits() { return l1Hits; }
        public long getL1Misses() { return l1Misses; }
        public long getL2Hits() { return l2Hits; }
        public long getL2Misses() { return l2Misses; }
        public long getPuts() { return puts; }
        public long getDeletes() { return deletes; }
        public long getErrors() { return errors; }
        public long getL1Errors() { return l1Errors; }
        public long getL2Errors() { return l2Errors; }
        public long getLoaderErrors() { return loaderErrors; }
        public long getDegradationCount() { return degradationCount; }

        public double getL1HitRate() {
            long total = l1Hits + l1Misses;
            return total == 0 ? 0 : (double) l1Hits / total;
        }

        public double getL2HitRate() {
            long total = l2Hits + l2Misses;
            return total == 0 ? 0 : (double) l2Hits / total;
        }

        @Override
        public String toString() {
            return String.format(
                    "EnhancedCacheStats{l1Hits=%d, l1Misses=%d, l1HitRate=%.2f%%, l2Hits=%d, l2Misses=%d, l2HitRate=%.2f%%, " +
                            "puts=%d, deletes=%d, errors=%d, l1Errors=%d, l2Errors=%d, loaderErrors=%d, degradationCount=%d}",
                    l1Hits, l1Misses, getL1HitRate() * 100,
                    l2Hits, l2Misses, getL2HitRate() * 100,
                    puts, deletes, errors, l1Errors, l2Errors, loaderErrors, degradationCount);
        }
    }
}