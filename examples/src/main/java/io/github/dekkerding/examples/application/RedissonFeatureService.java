package io.github.dekkerding.examples.application;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redisson功能服务
 *
 * <p>统一的Redisson高级功能入口，封装分布式集合、队列、原子操作等功能。
 *
 * <p>核心功能：
 * <ul>
 *   <li>分布式集合：RMap、RSet、RList、RScoredSortedSet</li>
 *   <li>分布式队列：RBlockingQueue、RDelayedQueue、RPriorityQueue</li>
   *   <li>原子操作：RAtomicLong、RBitSet、RHyperLogLog</li>
 *   <li>高级功能：限流、布隆过滤器、分布式锁</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RedissonFeatureService {

    @Autowired(required = false)
    private RedissonClient redisson;

    // ==================== 分布式集合 ====================

    /**
     * 获取分布式Map（带本地缓存）
     */
    public <K, V> RMap<K, V> getMap(String name) {
        assertRedissonAvailable();
        RMap<K, V> map = redisson.getMap(name);
        log.debug("创建RMap: {}", name);
        return map;
    }

    /**
     * 获取分布式Set
     */
    public <V> RSet<V> getSet(String name) {
        assertRedissonAvailable();
        RSet<V> set = redisson.getSet(name);
        log.debug("创建RSet: {}", name);
        return set;
    }

    /**
     * 获取分布式List
     */
    public <V> RList<V> getList(String name) {
        assertRedissonAvailable();
        RList<V> list = redisson.getList(name);
        log.debug("创建RList: {}", name);
        return list;
    }

    /**
     * 获取有序集合（排行榜）
     */
    public <V> RScoredSortedSet<V> getScoredSet(String name) {
        assertRedissonAvailable();
        RScoredSortedSet<V> sortedSet = redisson.getScoredSortedSet(name);
        log.debug("创建RScoredSortedSet: {}", name);
        return sortedSet;
    }

    // ==================== 分布式队列 ====================

    /**
     * 获取阻塞队列
     */
    public <V> RBlockingQueue<V> getBlockingQueue(String name) {
        assertRedissonAvailable();
        RBlockingQueue<V> queue = redisson.getBlockingQueue(name);
        log.debug("创建RBlockingQueue: {}", name);
        return queue;
    }

    /**
     * 获取延迟队列
     */
    public <V> RDelayedQueue<V> getDelayedQueue(String name) {
        assertRedissonAvailable();
        RQueue<V> destinationQueue = redisson.getQueue(name);
        RDelayedQueue<V> delayedQueue = redisson.getDelayedQueue(destinationQueue);
        log.debug("创建RDelayedQueue: {}", name);
        return delayedQueue;
    }

    /**
     * 获取优先级队列
     */
    public <V> RPriorityQueue<V> getPriorityQueue(String name) {
        assertRedissonAvailable();
        RPriorityQueue<V> priorityQueue = redisson.getPriorityQueue(name);
        log.debug("创建RPriorityQueue: {}", name);
        return priorityQueue;
    }

    // ==================== 原子操作 ====================

    /**
     * 获取原子长整型
     */
    public RAtomicLong getAtomicLong(String name) {
        assertRedissonAvailable();
        RAtomicLong atomicLong = redisson.getAtomicLong(name);
        log.debug("创建RAtomicLong: {}", name);
        return atomicLong;
    }

    /**
     * 生成分布式ID
     */
    public long generateId(String key) {
        RAtomicLong atomic = getAtomicLong("id:" + key);
        long id = atomic.incrementAndGet();
        log.debug("生成ID: {} = {}", key, id);
        return id;
    }

    /**
     * 获取位集合
     */
    public RBitSet getBitSet(String name) {
        assertRedissonAvailable();
        RBitSet bitSet = redisson.getBitSet(name);
        log.debug("创建RBitSet: {}", name);
        return bitSet;
    }

    /**
     * 获取基数估算（UV统计）
     */
    public <T> RHyperLogLog<T> getHyperLogLog(String name) {
        assertRedissonAvailable();
        RHyperLogLog<T> hyperLogLog = redisson.getHyperLogLog(name);
        log.debug("创建RHyperLogLog: {}", name);
        return hyperLogLog;
    }

    // ==================== 高级功能 ====================

    /**
     * 获取限流器
     */
    public RRateLimiter getRateLimiter(String name) {
        assertRedissonAvailable();
        RRateLimiter rateLimiter = redisson.getRateLimiter(name);
        log.debug("创建RRateLimiter: {}", name);
        return rateLimiter;
    }

    /**
     * 获取布隆过滤器
     */
    public <T> RBloomFilter<T> getBloomFilter(String name, long expectedInsertions, double falseProbability) {
        assertRedissonAvailable();
        RBloomFilter<T> bloomFilter = redisson.getBloomFilter(name);
        bloomFilter.tryInit(expectedInsertions, falseProbability);
        log.debug("创建RBloomFilter: {}, expectedInsertions={}, falseProbability={}",
                name, expectedInsertions, falseProbability);
        return bloomFilter;
    }

    /**
     * 获取分布式锁
     */
    public RLock getLock(String lockName) {
        assertRedissonAvailable();
        RLock lock = redisson.getLock(lockName);
        log.debug("创建RLock: {}", lockName);
        return lock;
    }

    // ==================== 工具方法 ====================

    private void assertRedissonAvailable() {
        if (redisson == null) {
            throw new IllegalStateException("Redisson客户端未配置，请检查配置");
        }
    }
}
