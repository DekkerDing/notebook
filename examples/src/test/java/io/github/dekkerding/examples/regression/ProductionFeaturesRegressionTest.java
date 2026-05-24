package io.github.dekkerding.examples.regression;

import io.github.dekkerding.examples.application.DistributedLockService;
import io.github.dekkerding.examples.application.IdGeneratorService;
import io.github.dekkerding.examples.application.RateLimiterService;
import io.github.dekkerding.examples.application.TwoLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.redisson.api.RateIntervalUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 生产级功能回归测试套件
 *
 * <p>测试目标：验证生产环境所需的核心功能完整性
 *
 * <p>测试范围：
 * <ul>
 *   <li>分布式锁（同步锁、读写锁、公平锁、锁回调）</li>
 *   <li>限流器（令牌桶、限流回调、预定义限流）</li>
 *   <li>ID生成器（序列ID、业务ID、雪花算法、时间戳ID）</li>
 *   <li>两级缓存（L1/L2、缓存穿透保护、统计信息）</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("生产级功能回归测试")
public class ProductionFeaturesRegressionTest {

    @Autowired(required = false)
    private DistributedLockService distributedLockService;

    @Autowired(required = false)
    private RateLimiterService rateLimiterService;

    @Autowired(required = false)
    private IdGeneratorService idGeneratorService;

    @Autowired(required = false)
    private TwoLevelCacheService cacheService;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(distributedLockService != null, "DistributedLockService 未配置");
        Assumptions.assumeTrue(rateLimiterService != null, "RateLimiterService 未配置");
        Assumptions.assumeTrue(idGeneratorService != null, "IdGeneratorService 未配置");
        Assumptions.assumeTrue(cacheService != null, "TwoLevelCacheService 未配置");
    }

    // ==================== 分布式锁测试 ====================

    @Test
    @Order(1)
    @DisplayName("LOCK-001: 基础锁获取与释放")
    void testBasicLock() {
        String lockKey = "test:basic:lock";

        try {
            // 获取锁
            boolean acquired = distributedLockService.tryLock(lockKey, 5, 10, TimeUnit.SECONDS);
            assertTrue(acquired, "应该成功获取锁");

            // 检查锁状态
            assertTrue(distributedLockService.isLocked(lockKey), "锁应该被持有");
            assertTrue(distributedLockService.isHeldByCurrentThread(lockKey), "锁应该由当前线程持有");

            // 释放锁
            distributedLockService.unlock(lockKey);

            // 检查锁已释放
            assertFalse(distributedLockService.isLocked(lockKey), "锁应该已释放");

            log.info("基础锁测试通过: key={}", lockKey);
        } finally {
            distributedLockService.forceUnlock(lockKey);
        }
    }

    @Test
    @Order(2)
    @DisplayName("LOCK-002: 锁超时与竞争")
    void testLockTimeout() throws Exception {
        String lockKey = "test:timeout:lock";
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch acquiredLatch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger(0);

        // 线程1：持有锁3秒
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                startLatch.await();
                if (distributedLockService.tryLock(lockKey, 5, 30, TimeUnit.SECONDS)) {
                    acquiredLatch.countDown();
                    Thread.sleep(3000);
                    distributedLockService.unlock(lockKey);
                    result.set(1);
                }
            } catch (Exception e) {
                log.error("线程1执行失败", e);
            }
        });

        // 线程2：等待1秒获取锁（应该失败）
        startLatch.countDown();
        acquiredLatch.await(5, TimeUnit.SECONDS);

        boolean acquired = distributedLockService.tryLock(lockKey, 1, 30, TimeUnit.SECONDS);
        assertFalse(acquired, "应该获取锁超时");

        // 等待线程1完成
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(1, result.get(), "线程1应该成功执行");

        distributedLockService.forceUnlock(lockKey);
        log.info("锁超时测试通过: key={}", lockKey);
    }

    @Test
    @Order(3)
    @DisplayName("LOCK-003: 锁回调模板")
    void testLockCallback() {
        String lockKey = "test:callback:lock";

        String result = distributedLockService.executeWithLock(lockKey, 10, () -> {
            return "success";
        });

        assertEquals("success", result, "回调应该正常执行");

        // 测试异常情况
        assertThrows(DistributedLockService.LockAcquisitionException.class, () -> {
            // 先获取锁
            distributedLockService.tryLock(lockKey, 30, 30, TimeUnit.SECONDS);
            try {
                // 尝试在超时时间内获取锁
                distributedLockService.executeWithLock(lockKey, 1, () -> "should fail");
            } finally {
                distributedLockService.unlock(lockKey);
            }
        });

        distributedLockService.forceUnlock(lockKey);
        log.info("锁回调测试通过: key={}", lockKey);
    }

    @Test
    @Order(4)
    @DisplayName("LOCK-004: 读写锁")
    void testReadWriteLock() {
        String lockKey = "test:rwlock:lock";

        try {
            // 获取读锁
            boolean readAcquired = distributedLockService.tryReadLock(lockKey, 5, 30, TimeUnit.SECONDS);
            assertTrue(readAcquired, "应该成功获取读锁");

            // 释放读锁
            distributedLockService.unlockReadLock(lockKey);

            // 获取写锁
            boolean writeAcquired = distributedLockService.tryWriteLock(lockKey, 5, 30, TimeUnit.SECONDS);
            assertTrue(writeAcquired, "应该成功获取写锁");

            // 释放写锁
            distributedLockService.unlockWriteLock(lockKey);

            log.info("读写锁测试通过: key={}", lockKey);
        } finally {
            distributedLockService.forceUnlock(lockKey);
        }
    }

    // ==================== 限流器测试 ====================

    @Test
    @Order(10)
    @DisplayName("RATE-001: 限流器初始化与配置")
    void testRateLimiterInit() {
        String limiterKey = "test:init:limiter";

        boolean initialized = rateLimiterService.initLimiter(limiterKey, 10, RateIntervalUnit.SECONDS);
        assertTrue(initialized, "应该成功初始化限流器");

        // 再次初始化应该失败（已存在）
        boolean reinitialized = rateLimiterService.initLimiter(limiterKey, 10, RateIntervalUnit.SECONDS);
        assertFalse(reinitialized, "限流器已存在，初始化应该失败");

        // 获取配置信息
        String config = rateLimiterService.getLimiterConfig(limiterKey);
        assertNotNull(config, "配置信息不应为空");
        log.info("限流器配置: {}", config);

        rateLimiterService.deleteLimiter(limiterKey);
        log.info("限流器初始化测试通过: key={}", limiterKey);
    }

    @Test
    @Order(11)
    @DisplayName("RATE-002: 令牌获取与限流")
    void testRateLimiterAcquire() {
        String limiterKey = "test:acquire:limiter";

        // 初始化：每秒5个令牌
        rateLimiterService.initLimiter(limiterKey, 5, RateIntervalUnit.SECONDS);

        // 获取5个令牌（应该成功）
        for (int i = 0; i < 5; i++) {
            boolean acquired = rateLimiterService.tryAcquire(limiterKey);
            assertTrue(acquired, "第" + (i + 1) + "个令牌应该获取成功");
        }

        // 第6个令牌应该失败（限流）
        boolean acquired = rateLimiterService.tryAcquire(limiterKey);
        assertFalse(acquired, "第6个令牌应该获取失败（限流）");

        rateLimiterService.deleteLimiter(limiterKey);
        log.info("令牌获取测试通过: key={}", limiterKey);
    }

    @Test
    @Order(12)
    @DisplayName("RATE-003: 限流回调模板")
    void testRateLimiterCallback() {
        String limiterKey = "test:callback:limiter";

        // 初始化：每秒10个令牌
        rateLimiterService.initLimiter(limiterKey, 10, RateIntervalUnit.SECONDS);

        // 正常执行
        String result = rateLimiterService.executeWithRateLimit(
                limiterKey,
                () -> "success",
                () -> "fallback"
        );
        assertEquals("success", result, "应该正常执行");

        // 消耗所有令牌
        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryAcquire(limiterKey);
        }

        // 触发限流降级
        result = rateLimiterService.executeWithRateLimit(
                limiterKey,
                () -> "should not execute",
                () -> "fallback"
        );
        assertEquals("fallback", result, "应该执行降级逻辑");

        rateLimiterService.deleteLimiter(limiterKey);
        log.info("限流回调测试通过: key={}", limiterKey);
    }

    @Test
    @Order(13)
    @DisplayName("RATE-004: 预定义限流器")
    void testPredefinedRateLimiters() {
        // API限流
        boolean apiAllowed = rateLimiterService.apiRateLimit("test-api");
        assertTrue(apiAllowed, "API限流应该通过");

        // 用户限流
        boolean userAllowed = rateLimiterService.userRateLimit("user123");
        assertTrue(userAllowed, "用户限流应该通过");

        // IP限流
        boolean ipAllowed = rateLimiterService.ipRateLimit("192.168.1.1");
        assertTrue(ipAllowed, "IP限流应该通过");

        log.info("预定义限流器测试通过");
    }

    // ==================== ID生成器测试 ====================

    @Test
    @Order(20)
    @DisplayName("ID-001: 序列ID生成")
    void testSequenceId() {
        String key = "test:sequence:id";

        // 重置序列
        idGeneratorService.setId(key, 0);

        // 生成ID
        long id1 = idGeneratorService.nextId(key);
        assertEquals(1, id1, "第一个ID应该是1");

        long id2 = idGeneratorService.nextId(key);
        assertEquals(2, id2, "第二个ID应该是2");

        // 批量获取
        long batchStart = idGeneratorService.nextIdBatch(key, 10);
        assertEquals(3, batchStart, "批量起始ID应该是3");

        log.info("序列ID测试通过: key={}, ids=1,2,batch(3-12)", key);
    }

    @Test
    @Order(21)
    @DisplayName("ID-002: 业务ID生成")
    void testBusinessId() {
        String orderId1 = idGeneratorService.nextBusinessId("ORD", "test:order");
        String orderId2 = idGeneratorService.nextBusinessId("ORD", "test:order");

        assertTrue(orderId1.startsWith("ORD"), "订单ID应该以ORD开头");
        assertTrue(orderId2.startsWith("ORD"), "订单ID应该以ORD开头");

        assertNotEquals(orderId1, orderId2, "两个订单ID应该不同");

        log.info("业务ID测试通过: orderId1={}, orderId2={}", orderId1, orderId2);
    }

    @Test
    @Order(22)
    @DisplayName("ID-003: 雪花算法ID")
    void testSnowflakeId() {
        String key = "test:snowflake:id";

        long id1 = idGeneratorService.snowflakeId(key);
        long id2 = idGeneratorService.snowflakeId(key);

        assertTrue(id1 > 0, "雪花ID应该大于0");
        assertTrue(id2 > id1, "后续雪花ID应该大于之前的");

        log.info("雪花ID测试通过: id1={}, id2={}", id1, id2);
    }

    @Test
    @Order(23)
    @DisplayName("ID-004: 时间戳ID")
    void testTimestampId() {
        String key = "test:timestamp:id";

        String id1 = idGeneratorService.timestampId(key);
        String id2 = idGeneratorService.timestampId(key);

        assertTrue(id1.length() >= 14, "时间戳ID应该至少14位");
        assertTrue(id2.length() >= 14, "时间戳ID应该至少14位");

        assertNotEquals(id1, id2, "两个时间戳ID应该不同");

        log.info("时间戳ID测试通过: id1={}, id2={}", id1, id2);
    }

    @Test
    @Order(24)
    @DisplayName("ID-005: 预定义业务ID")
    void testPredefinedBusinessIds() {
        String orderId = idGeneratorService.orderId();
        String paymentId = idGeneratorService.paymentId();
        String refundId = idGeneratorService.refundId();
        String userId = idGeneratorService.userId();
        String txnId = idGeneratorService.transactionId();

        assertTrue(orderId.startsWith("ORD"), "订单ID应该以ORD开头");
        assertTrue(paymentId.startsWith("PAY"), "支付ID应该以PAY开头");
        assertTrue(refundId.startsWith("REF"), "退款ID应该以REF开头");
        assertTrue(userId.startsWith("USR"), "用户ID应该以USR开头");
        assertTrue(txnId.startsWith("TXN"), "交易ID应该以TXN开头");

        log.info("预定义业务ID测试通过: orderId={}, paymentId={}, refundId={}, userId={}, txnId={}",
                orderId, paymentId, refundId, userId, txnId);
    }

    // ==================== 两级缓存测试 ====================

    @Test
    @Order(30)
    @DisplayName("CACHE-001: 基础缓存操作")
    void testBasicCache() {
        String key = "test:basic:cache";
        TestUser user = new TestUser("user123", "Test User");

        // 设置缓存
        cacheService.set(key, user, 5, TimeUnit.MINUTES);

        // 获取缓存
        TestUser cached = cacheService.get(key, TestUser.class);
        assertNotNull(cached, "缓存不应为空");
        assertEquals("user123", cached.getId(), "用户ID应该匹配");
        assertEquals("Test User", cached.getName(), "用户名应该匹配");

        // 删除缓存
        cacheService.delete(key);
        TestUser deleted = cacheService.get(key, TestUser.class);
        assertNull(deleted, "删除后缓存应该为空");

        log.info("基础缓存测试通过: key={}", key);
    }

    @Test
    @Order(31)
    @DisplayName("CACHE-002: 缓存加载器")
    void testCacheLoader() {
        String key = "test:loader:cache";

        // 清空缓存
        cacheService.delete(key);

        // 使用加载器获取缓存（应该调用加载器）
        TestUser user = cacheService.get(key, TestUser.class, () -> {
            return new TestUser("loaded-user", "Loaded User");
        });

        assertNotNull(user, "用户不应为空");
        assertEquals("loaded-user", user.getId(), "应该返回加载的数据");

        // 再次获取应该命中缓存（不会调用加载器）
        TestUser cached = cacheService.get(key, TestUser.class, () -> {
            return new TestUser("should-not-load", "Should Not Load");
        });

        assertEquals("loaded-user", cached.getId(), "应该返回缓存的数据");

        cacheService.delete(key);
        log.info("缓存加载器测试通过: key={}", key);
    }

    @Test
    @Order(32)
    @DisplayName("CACHE-003: 缓存统计")
    void testCacheStats() {
        String key = "test:stats:cache";

        // 重置统计
        cacheService.resetStats();

        // 清空缓存
        cacheService.delete(key);

        // 第一次获取（L1、L2都未命中）
        TestUser user1 = cacheService.get(key, TestUser.class, () -> new TestUser("stats-user", "Stats User"));
        assertNotNull(user1);

        // 第二次获取（L1命中）
        TestUser user2 = cacheService.get(key, TestUser.class);
        assertNotNull(user2);

        // 获取统计信息
        TwoLevelCacheService.CacheStats stats = cacheService.getStats();

        assertTrue(stats.getL1Hits() >= 1, "应该有L1命中");
        assertTrue(stats.getPuts() >= 1, "应该有PUT操作");

        log.info("缓存统计测试通过: stats={}", stats);
        cacheService.delete(key);
    }

    @Test
    @Order(33)
    @DisplayName("CACHE-004: 批量删除")
    void testBatchDelete() {
        cacheService.set("test:batch:1", "value1");
        cacheService.set("test:batch:2", "value2");
        cacheService.set("test:batch:3", "value3");

        // 验证缓存存在
        assertTrue(cacheService.exists("test:batch:1"));
        assertTrue(cacheService.exists("test:batch:2"));
        assertTrue(cacheService.exists("test:batch:3"));

        // 批量删除
        cacheService.deleteBatch(java.util.Arrays.asList("test:batch:1", "test:batch:2"));

        // 验证部分删除
        assertFalse(cacheService.exists("test:batch:1"));
        assertFalse(cacheService.exists("test:batch:2"));
        assertTrue(cacheService.exists("test:batch:3"));

        // 清理
        cacheService.delete("test:batch:3");
        log.info("批量删除测试通过");
    }

    // ==================== 综合场景测试 ====================

    @Test
    @Order(40)
    @DisplayName("SCENARIO-001: 订单处理综合场景")
    void testOrderProcessingScenario() {
        String orderId = idGeneratorService.orderId();
        String lockKey = "order:" + orderId;
        String limiterKey = "order:process";
        String cacheKey = "order:detail:" + orderId;

        try {
            // 1. 初始化限流器
            rateLimiterService.initLimiter(limiterKey, 10, RateIntervalUnit.SECONDS);

            // 2. 限流检查
            boolean allowed = rateLimiterService.tryAcquire(limiterKey);
            assertTrue(allowed, "限流应该通过");

            // 3. 获取分布式锁
            boolean locked = distributedLockService.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
            assertTrue(locked, "应该成功获取锁");

            // 4. 创建订单对象
            TestOrder order = new TestOrder(orderId, "PENDING", 1000L);

            // 5. 缓存订单详情
            cacheService.set(cacheKey, order, 30, TimeUnit.MINUTES);

            // 6. 从缓存读取
            TestOrder cachedOrder = cacheService.get(cacheKey, TestOrder.class);
            assertNotNull(cachedOrder, "缓存的订单不应为空");
            assertEquals(orderId, cachedOrder.getOrderId(), "订单ID应该匹配");

            // 7. 释放锁
            distributedLockService.unlock(lockKey);

            log.info("订单处理综合场景测试通过: orderId={}", orderId);

        } finally {
            distributedLockService.forceUnlock(lockKey);
            rateLimiterService.deleteLimiter(limiterKey);
            cacheService.delete(cacheKey);
        }
    }

    // ==================== 测试模型类 ====================

    static class TestUser {
        private String id;
        private String name;

        public TestUser() {
        }

        public TestUser(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class TestOrder {
        private String orderId;
        private String status;
        private Long amount;

        public TestOrder() {
        }

        public TestOrder(String orderId, String status, Long amount) {
            this.orderId = orderId;
            this.status = status;
            this.amount = amount;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getStatus() {
            return status;
        }

        public Long getAmount() {
            return amount;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }
    }
}
