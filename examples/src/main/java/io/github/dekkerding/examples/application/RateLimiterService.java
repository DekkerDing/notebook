package io.github.dekkerding.examples.application;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 限流器服务
 *
 * <p>基于Redisson实现的分布式限流器服务，提供生产级的限流功能。
 *
 * <p>核心功能：
 * <ul>
 *   <li>令牌桶算法限流</li>
 *   <li>滑动窗口算法限流</li>
 *   <li>动态限流配置</li>
 *   <li>限流统计与监控</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 初始化限流器：每秒100个请求
 * rateLimiterService.initLimiter("api:user:create", 100, RateIntervalUnit.SECONDS);
 *
 * // 尝试获取令牌
 * if (rateLimiterService.tryAcquire("api:user:create")) {
 *     // 处理请求
 * } else {
 *     // 触发限流，返回错误
 * }
 *
 * // 使用限流回调模板
 * rateLimiterService.executeWithRateLimit("api:user:create", () -> {
 *     // 处理请求
 *     return result;
 * }, () -> {
 *     // 限流降级逻辑
 *     return fallbackResult;
 * });
 * }</pre>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RateLimiterService {

    @Autowired(required = false)
    private RedissonClient redisson;

    private static final String LIMITER_KEY_PREFIX = "ratelimit:";

    /**
     * 默认限流速率
     */
    private static final long DEFAULT_RATE = 100L;

    /**
     * 默认时间间隔
     */
    private static final RateIntervalUnit DEFAULT_INTERVAL = RateIntervalUnit.SECONDS;

    // ==================== 初始化与配置 ====================

    /**
     * 初始化限流器（令牌桶算法）
     *
     * <p>使用OVERALL模式，全局限流。
     *
     * @param limiterKey  限流器Key（业务标识）
     * @param rate        限流速率（每interval个时间单位允许的请求数）
     * @param interval    时间间隔单位
     * @return 是否初始化成功
     */
    public boolean initLimiter(String limiterKey, long rate, RateIntervalUnit interval) {
        return initLimiter(limiterKey, RateType.OVERALL, rate, interval);
    }

    /**
     * 初始化限流器（指定模式）
     *
     * @param limiterKey  限流器Key（业务标识）
     * @param rateType    限流类型（OVERALL=全局限流，PER_CLIENT=单客户端限流）
     * @param rate        限流速率
     * @param interval    时间间隔单位
     * @return 是否初始化成功
     */
    public boolean initLimiter(String limiterKey, RateType rateType, long rate, RateIntervalUnit interval) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = getRateLimiter(limiterKey);

        // rateIntervalUnit转换为数值：SECONDS=1, MINUTES=2, HOURS=3, DAYS=4
        int intervalOrdinal = 1; // 默认SECONDS
        if (interval == RateIntervalUnit.MINUTES) {
            intervalOrdinal = 2;
        } else if (interval == RateIntervalUnit.HOURS) {
            intervalOrdinal = 3;
        } else if (interval == RateIntervalUnit.DAYS) {
            intervalOrdinal = 4;
        }

        boolean initialized = rateLimiter.trySetRate(rateType, rate, intervalOrdinal, interval);
        if (initialized) {
            log.info("限流器初始化成功: key={}, rateType={}, rate={}{}",
                    buildFullKey(limiterKey), rateType, rate, interval);
        } else {
            log.info("限流器已存在: key={}", buildFullKey(limiterKey));
        }

        return initialized;
    }

    /**
     * 更新限流器配置
     *
     * <p>如果限流器不存在，会自动创建。
     *
     * @param limiterKey  限流器Key（业务标识）
     * @param rate        新的限流速率
     * @param interval    新的时间间隔单位
     * @return 是否更新成功
     */
    public boolean updateLimiter(String limiterKey, long rate, RateIntervalUnit interval) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = getRateLimiter(limiterKey);

        // 先删除现有配置，再重新设置
        rateLimiter.delete();

        int intervalOrdinal = 1; // 默认SECONDS
        if (interval == RateIntervalUnit.MINUTES) {
            intervalOrdinal = 2;
        } else if (interval == RateIntervalUnit.HOURS) {
            intervalOrdinal = 3;
        } else if (interval == RateIntervalUnit.DAYS) {
            intervalOrdinal = 4;
        }

        boolean updated = rateLimiter.trySetRate(RateType.OVERALL, rate, intervalOrdinal, interval);

        if (updated) {
            log.info("限流器配置已更新: key={}, rate={}{}", buildFullKey(limiterKey), rate, interval);
        }

        return updated;
    }

    // ==================== 令牌获取 ====================

    /**
     * 尝试获取1个令牌
     *
     * @param limiterKey  限流器Key（业务标识）
     * @return 是否获取成功
     */
    public boolean tryAcquire(String limiterKey) {
        return tryAcquire(limiterKey, 1);
    }

    /**
     * 尝试获取指定数量令牌
     *
     * @param limiterKey  限流器Key（业务标识）
     * @param permits     需要获取的令牌数
     * @return 是否获取成功
     */
    public boolean tryAcquire(String limiterKey, long permits) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = getRateLimiter(limiterKey);

        boolean acquired = rateLimiter.tryAcquire(permits);
        if (!acquired) {
            log.debug("限流触发: key={}, permits={}", buildFullKey(limiterKey), permits);
        }

        return acquired;
    }

    /**
     * 尝试在指定时间内获取令牌
     *
     * @param limiterKey  限流器Key（业务标识）
     * @param permits     需要获取的令牌数
     * @param timeout     超时时间
     * @param timeUnit    时间单位
     * @return 是否获取成功
     */
    public boolean tryAcquire(String limiterKey, long permits, long timeout, TimeUnit timeUnit) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = getRateLimiter(limiterKey);

        boolean acquired = rateLimiter.tryAcquire(permits, timeout, timeUnit);
        if (!acquired) {
            log.debug("限流触发（超时）: key={}, permits={}, timeout={}{}",
                    buildFullKey(limiterKey), permits, timeout, timeUnit);
        }
        return acquired;
    }

    // ==================== 限流回调模板 ====================

    /**
     * 执行带限流保护的操作（无返回值）
     *
     * @param limiterKey    限流器Key（业务标识）
     * @param callback      正常执行回调
     * @param fallback      限流时降级回调
     */
    public void executeWithRateLimit(String limiterKey, Runnable callback, Runnable fallback) {
        executeWithRateLimit(limiterKey, 1, () -> {
            callback.run();
            return null;
        }, () -> {
            fallback.run();
            return null;
        });
    }

    /**
     * 执行带限流保护的操作（有返回值）
     *
     * @param limiterKey    限流器Key（业务标识）
     * @param callback      正常执行回调
     * @param fallback      限流时降级回调
     * @param <T>           返回值类型
     * @return 回调返回值
     */
    public <T> T executeWithRateLimit(String limiterKey, Supplier<T> callback, Supplier<T> fallback) {
        return executeWithRateLimit(limiterKey, 1, callback, fallback);
    }

    /**
     * 执行带限流保护的操作（指定令牌数）
     *
     * @param limiterKey    限流器Key（业务标识）
     * @param permits       需要的令牌数
     * @param callback      正常执行回调
     * @param fallback      限流时降级回调
     * @param <T>           返回值类型
     * @return 回调返回值
     */
    public <T> T executeWithRateLimit(String limiterKey, long permits, Supplier<T> callback, Supplier<T> fallback) {
        if (tryAcquire(limiterKey, permits)) {
            return callback.get();
        } else {
            log.warn("限流降级: key={}, permits={}", buildFullKey(limiterKey), permits);
            return fallback.get();
        }
    }

    // ==================== 限流统计 ====================

    /**
     * 获取限流器配置信息
     *
     * @param limiterKey  限流器Key（业务标识）
     * @return 配置信息字符串
     */
    public String getLimiterConfig(String limiterKey) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = getRateLimiter(limiterKey);

        return String.format("RateLimiter[key=%s, availablePermits=%d]",
                buildFullKey(limiterKey),
                rateLimiter.availablePermits());
    }

    /**
     * 获取当前可用令牌数
     *
     * @param limiterKey  限流器Key（业务标识）
     * @return 可用令牌数
     */
    public long getAvailablePermits(String limiterKey) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = getRateLimiter(limiterKey);
        return rateLimiter.availablePermits();
    }

    /**
     * 删除限流器
     *
     * @param limiterKey  限流器Key（业务标识）
     * @return 是否删除成功
     */
    public boolean deleteLimiter(String limiterKey) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = getRateLimiter(limiterKey);
        boolean deleted = rateLimiter.delete();

        if (deleted) {
            log.info("限流器已删除: key={}", buildFullKey(limiterKey));
        }

        return deleted;
    }

    // ==================== 预定义限流器 ====================

    /**
     * API限流：每秒100次
     *
     * @param apiKey  API标识
     * @return 是否通过限流
     */
    public boolean apiRateLimit(String apiKey) {
        String limiterKey = "api:" + apiKey;
        if (!exists(limiterKey)) {
            initLimiter(limiterKey, 100, RateIntervalUnit.SECONDS);
        }
        return tryAcquire(limiterKey);
    }

    /**
     * 用户限流：每秒10次
     *
     * @param userId  用户ID
     * @return 是否通过限流
     */
    public boolean userRateLimit(String userId) {
        String limiterKey = "user:" + userId;
        if (!exists(limiterKey)) {
            initLimiter(limiterKey, 10, RateIntervalUnit.SECONDS);
        }
        return tryAcquire(limiterKey);
    }

    /**
     * IP限流：每秒20次
     *
     * @param ip  IP地址
     * @return 是否通过限流
     */
    public boolean ipRateLimit(String ip) {
        String limiterKey = "ip:" + ip;
        if (!exists(limiterKey)) {
            initLimiter(limiterKey, 20, RateIntervalUnit.SECONDS);
        }
        return tryAcquire(limiterKey);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取限流器对象
     */
    private RRateLimiter getRateLimiter(String limiterKey) {
        return redisson.getRateLimiter(buildFullKey(limiterKey));
    }

    /**
     * 构建完整的限流器Key
     */
    private String buildFullKey(String limiterKey) {
        return LIMITER_KEY_PREFIX + limiterKey;
    }

    /**
     * 检查限流器是否存在
     */
    private boolean exists(String limiterKey) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = getRateLimiter(limiterKey);
        return rateLimiter.isExists();
    }

    /**
     * 断言Redisson可用
     */
    private void assertRedissonAvailable() {
        if (redisson == null) {
            throw new IllegalStateException("Redisson客户端未配置，限流器功能不可用");
        }
    }

    // ==================== 异常类 ====================

    /**
     * 限流异常
     */
    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }

        public RateLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
