package io.github.dekkerding.examples.application;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务
 *
 * <p>基于Redisson实现的分布式锁服务，提供生产级的锁功能。
 *
 * <p>核心功能：
 * <ul>
 *   <li>同步锁获取与释放</li>
 *   <li>锁超时自动续期（看门狗）</li>
 *   <li>可重入锁支持</li>
 *   <li>读写锁支持</li>
 *   <li>锁回调模板</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 方式1: 手动加锁
 * String lockKey = "order:123";
 * if (distributedLockService.tryLock(lockKey, 10, 30, TimeUnit.SECONDS)) {
 *     try {
 *         // 业务逻辑
 *     } finally {
 *         distributedLockService.unlock(lockKey);
 *     }
 * }
 *
 * // 方式2: 使用回调模板
 * distributedLockService.executeWithLock("order:123", 30, TimeUnit.SECONDS, () -> {
 *     // 业务逻辑
 *     return result;
 * });
 * }</pre>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class DistributedLockService {

    @Autowired(required = false)
    private RedissonClient redisson;

    private static final String LOCK_KEY_PREFIX = "lock:";

    /**
     * 默认锁等待时间（秒）
     */
    private static final long DEFAULT_WAIT_TIME = 10L;

    /**
     * 默认锁持有时间（秒），-1表示启用看门狗自动续期
     */
    private static final long DEFAULT_LEASE_TIME = -1L;

    // ==================== 基础锁操作 ====================

    /**
     * 尝试获取锁（使用默认超时配置）
     *
     * @param lockKey 锁的Key（业务标识）
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey    锁的Key（业务标识）
     * @param waitTime   等待获取锁的时间
     * @param leaseTime  锁持有时间，-1表示启用看门狗自动续期
     * @param timeUnit   时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        assertRedissonAvailable();
        RLock lock = getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired) {
                log.debug("锁获取成功: key={}, thread={}", buildFullKey(lockKey), Thread.currentThread().getId());
            } else {
                log.warn("锁获取失败: key={}, waitTime={}{}", buildFullKey(lockKey), waitTime, timeUnit);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("锁获取被中断: " + lockKey, e);
        } catch (Exception e) {
            throw new LockAcquisitionException("锁获取异常: " + lockKey, e);
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的Key（业务标识）
     */
    public void unlock(String lockKey) {
        assertRedissonAvailable();
        RLock lock = getLock(lockKey);

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("锁释放成功: key={}", buildFullKey(lockKey));
        } else {
            log.warn("尝试释放未持有的锁: key={}", buildFullKey(lockKey));
        }
    }

    /**
     * 强制释放锁（忽略持有者检查）
     *
     * <p>警告：此方法会强制释放锁，可能导致其他持有者的锁被误释放。
     * 仅在异常恢复场景下使用。
     *
     * @param lockKey 锁的Key（业务标识）
     */
    public void forceUnlock(String lockKey) {
        assertRedissonAvailable();
        RLock lock = getLock(lockKey);
        lock.forceUnlock();
        log.warn("强制释放锁: key={}", buildFullKey(lockKey));
    }

    /**
     * 检查锁是否被持有
     *
     * @param lockKey 锁的Key（业务标识）
     * @return 锁是否被持有
     */
    public boolean isLocked(String lockKey) {
        assertRedissonAvailable();
        RLock lock = getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * 检查锁是否由当前线程持有
     *
     * @param lockKey 锁的Key（业务标识）
     * @return 锁是否由当前线程持有
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        assertRedissonAvailable();
        RLock lock = getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    // ==================== 锁回调模板 ====================

    /**
     * 执行带锁的操作（无返回值）
     *
     * @param lockKey       锁的Key（业务标识）
     * @param leaseTime     锁持有时间（秒），-1表示启用看门狗
     * @param callback      回调函数
     */
    public void executeWithLock(String lockKey, long leaseTime, Runnable callback) {
        executeWithLock(lockKey, DEFAULT_WAIT_TIME, leaseTime, TimeUnit.SECONDS, () -> {
            callback.run();
            return null;
        });
    }

    /**
     * 执行带锁的操作（有返回值）
     *
     * @param lockKey       锁的Key（业务标识）
     * @param leaseTime     锁持有时间（秒），-1表示启用看门狗
     * @param callback      回调函数
     * @param <T>           返回值类型
     * @return 回调函数的返回值
     */
    public <T> T executeWithLock(String lockKey, long leaseTime, Supplier<T> callback) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, leaseTime, TimeUnit.SECONDS, callback);
    }

    /**
     * 执行带锁的操作（完整参数）
     *
     * @param lockKey       锁的Key（业务标识）
     * @param waitTime      等待获取锁的时间
     * @param leaseTime     锁持有时间，-1表示启用看门狗
     * @param timeUnit      时间单位
     * @param callback      回调函数
     * @param <T>           返回值类型
     * @return 回调函数的返回值
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> callback) {
        boolean locked = false;
        try {
            locked = tryLock(lockKey, waitTime, leaseTime, timeUnit);
            if (!locked) {
                throw new LockAcquisitionException("获取锁超时: " + lockKey + ", waitTime=" + waitTime + timeUnit);
            }
            return callback.get();
        } finally {
            if (locked) {
                unlock(lockKey);
            }
        }
    }

    // ==================== 读写锁 ====================

    /**
     * 获取读锁
     *
     * @param lockKey    锁的Key（业务标识）
     * @param waitTime   等待时间
     * @param leaseTime  持有时间
     * @param timeUnit   时间单位
     * @return 是否成功获取读锁
     */
    public boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        assertRedissonAvailable();
        RLock lock = redisson.getReadWriteLock(buildFullKey(lockKey)).readLock();

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired) {
                log.debug("读锁获取成功: key={}", buildFullKey(lockKey));
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("读锁获取被中断: " + lockKey, e);
        }
    }

    /**
     * 释放读锁
     *
     * @param lockKey 锁的Key（业务标识）
     */
    public void unlockReadLock(String lockKey) {
        assertRedissonAvailable();
        RLock lock = redisson.getReadWriteLock(buildFullKey(lockKey)).readLock();
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("读锁释放成功: key={}", buildFullKey(lockKey));
        }
    }

    /**
     * 获取写锁
     *
     * @param lockKey    锁的Key（业务标识）
     * @param waitTime   等待时间
     * @param leaseTime  持有时间
     * @param timeUnit   时间单位
     * @return 是否成功获取写锁
     */
    public boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        assertRedissonAvailable();
        RLock lock = redisson.getReadWriteLock(buildFullKey(lockKey)).writeLock();

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired) {
                log.debug("写锁获取成功: key={}", buildFullKey(lockKey));
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("写锁获取被中断: " + lockKey, e);
        }
    }

    /**
     * 释放写锁
     *
     * @param lockKey 锁的Key（业务标识）
     */
    public void unlockWriteLock(String lockKey) {
        assertRedissonAvailable();
        RLock lock = redisson.getReadWriteLock(buildFullKey(lockKey)).writeLock();
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("写锁释放成功: key={}", buildFullKey(lockKey));
        }
    }

    // ==================== 公平锁 ====================

    /**
     * 获取公平锁
     *
     * <p>公平锁按照请求锁的顺序获取锁，避免饥饿现象。
     *
     * @param lockKey    锁的Key（业务标识）
     * @param waitTime   等待时间
     * @param leaseTime  持有时间
     * @param timeUnit   时间单位
     * @return 是否成功获取公平锁
     */
    public boolean tryFairLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        assertRedissonAvailable();
        RLock lock = redisson.getFairLock(buildFullKey(lockKey));

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired) {
                log.debug("公平锁获取成功: key={}", buildFullKey(lockKey));
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("公平锁获取被中断: " + lockKey, e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取锁对象
     */
    private RLock getLock(String lockKey) {
        return redisson.getLock(buildFullKey(lockKey));
    }

    /**
     * 构建完整的锁Key
     */
    private String buildFullKey(String lockKey) {
        return LOCK_KEY_PREFIX + lockKey;
    }

    /**
     * 断言Redisson可用
     */
    private void assertRedissonAvailable() {
        if (redisson == null) {
            throw new IllegalStateException("Redisson客户端未配置，分布式锁功能不可用");
        }
    }

    // ==================== 异常类 ====================

    /**
     * 锁获取异常
     */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
