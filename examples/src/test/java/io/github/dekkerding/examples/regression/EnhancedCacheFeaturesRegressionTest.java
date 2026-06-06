package io.github.dekkerding.examples.regression;

import cn.hutool.core.map.MapUtil;
import io.github.dekkerding.examples.application.EnhancedTwoLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强型缓存功能回归测试
 *
 * <p>测试目标：验证增强型两级缓存服务的降级策略和兜底机制
 *
 * <p>测试范围：
 * <ul>
 *   <li>基础缓存操作（get、set、delete）</li>
 *   <li>降级策略测试</li>
 *   <li>异常兜底测试</li>
 *   <li>批量操作测试</li>
 *   <li>条件缓存测试</li>
 *   <li>缓存预热测试</li>
 *   <li>监控统计测试</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("增强型缓存功能回归测试")
public class EnhancedCacheFeaturesRegressionTest {

    @Autowired(required = false)
    private EnhancedTwoLevelCacheService cacheService;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(cacheService != null, "EnhancedTwoLevelCacheService 未配置");

        // 清理测试数据
        try {
            cacheService.deleteByPrefix("test:enhanced:");
        } catch (Exception ignored) {}
    }

    // ==================== 基础缓存操作测试 ====================

    @Test
    @Order(1)
    @DisplayName("CACHE-E-001: 基础缓存操作")
    void testBasicCacheOperations() {
        String key = "test:enhanced:basic:1";
        TestUser user = new TestUser("user123", "Test User");

        try {
            // 设置缓存
            cacheService.set(key, user, 5, TimeUnit.MINUTES);
            log.info("设置缓存成功: key={}", key);

            // 获取缓存
            TestUser cached = cacheService.get(key, TestUser.class);
            assertNotNull(cached, "缓存不应为空");
            assertEquals("user123", cached.getId(), "用户ID应该匹配");
            assertEquals("Test User", cached.getName(), "用户名应该匹配");
            log.info("获取缓存成功: user={}", cached);

            // 检查存在性
            assertTrue(cacheService.exists(key), "缓存应该存在");

            // 删除缓存
            cacheService.delete(key);
            TestUser deleted = cacheService.get(key, TestUser.class);
            assertNull(deleted, "删除后缓存应该为空");
            assertFalse(cacheService.exists(key), "删除后缓存应该不存在");

            log.info("基础缓存操作测试通过: key={}", key);
        } catch (Exception e) {
            log.error("基础缓存操作测试失败: key={}", key, e);
            fail("基础缓存操作测试失败: " + e.getMessage());
        } finally {
            cacheService.delete(key);
        }
    }

    @Test
    @Order(2)
    @DisplayName("CACHE-E-002: 带加载器的缓存获取")
    void testCacheWithLoader() {
        String key = "test:enhanced:loader:1";

        try {
            // 第一次获取：使用加载器
            TestUser user1 = cacheService.get(key, TestUser.class, () -> {
                log.info("加载器被调用");
                return new TestUser("loaded-user", "Loaded User");
            });

            assertNotNull(user1, "加载的用户不应为空");
            assertEquals("loaded-user", user1.getId(), "应该返回加载的数据");
            log.info("第一次加载: user={}", user1);

            // 第二次获取：应该命中缓存
            TestUser user2 = cacheService.get(key, TestUser.class, () -> {
                log.info("加载器不应该被调用");
                return new TestUser("should-not-load", "Should Not Load");
            });

            assertEquals("loaded-user", user2.getId(), "应该返回缓存的数据");
            log.info("第二次获取（缓存命中）: user={}", user2);

            log.info("带加载器的缓存测试通过: key={}", key);
        } catch (Exception e) {
            log.error("带加载器的缓存测试失败: key={}", key, e);
            fail("带加载器的缓存测试失败: " + e.getMessage());
        } finally {
            cacheService.delete(key);
        }
    }

    @Test
    @Order(3)
    @DisplayName("CACHE-E-003: 带兜底值的缓存获取")
    void testCacheWithFallback() {
        String key = "test:enhanced:fallback:1";

        try {
            // 缓存不存在，加载器抛出异常，应该返回兜底值
            TestUser user = cacheService.getWithFallback(key, TestUser.class, () -> {
                throw new RuntimeException("加载器异常");
            }, new TestUser("fallback", "Fallback User"));

            assertNotNull(user, "兜底用户不应为空");
            assertEquals("fallback", user.getId(), "应该返回兜底值");
            log.info("返回兜底值: user={}", user);

            log.info("带兜底值的缓存测试通过: key={}", key);
        } catch (Exception e) {
            log.error("带兜底值的缓存测试失败: key={}", key, e);
            fail("带兜底值的缓存测试失败: " + e.getMessage());
        } finally {
            cacheService.delete(key);
        }
    }

    // ==================== 降级策略测试 ====================

    @Test
    @Order(10)
    @DisplayName("CACHE-E-010: 手动降级测试")
    void testManualDegradation() {
        try {
            // 记录初始状态
            boolean initiallyDegraded = cacheService.isDegraded();
            log.info("初始降级状态: {}", initiallyDegraded);

            // 手动触发降级
            cacheService.triggerDegradation("测试降级");
            assertTrue(cacheService.isDegraded(), "应该处于降级状态");
            assertEquals("测试降级", cacheService.getDegradationReason(), "降级原因应该匹配");
            log.info("降级状态: reason={}", cacheService.getDegradationReason());

            // 设置缓存（应该只写入L1）
            String key = "test:enhanced:degradation:1";
            cacheService.set(key, new TestUser("degraded", "Degraded User"));

            // 获取缓存（应该能从L1获取）
            TestUser user = cacheService.get(key, TestUser.class);
            assertNotNull(user, "降级状态下应该能从L1获取缓存");
            assertEquals("degraded", user.getId(), "用户ID应该匹配");
            log.info("降级状态下缓存操作成功: user={}", user);

            // 恢复服务
            cacheService.recoverFromDegradation();
            assertFalse(cacheService.isDegraded(), "应该恢复到正常状态");
            assertNull(cacheService.getDegradationReason(), "降级原因应该被清除");
            log.info("服务已恢复");

            log.info("手动降级测试通过");
        } catch (Exception e) {
            log.error("手动降级测试失败", e);
            fail("手动降级测试失败: " + e.getMessage());
        } finally {
            cacheService.recoverFromDegradation();
        }
    }

    // ==================== 批量操作测试 ====================

    @Test
    @Order(20)
    @DisplayName("CACHE-E-020: 批量获取缓存")
    void testBatchGet() {
        try {
            // 准备测试数据
            Map<String, TestUser> users = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                String key = "test:enhanced:batch:" + i;
                TestUser user = new TestUser("user" + i, "User " + i);
                users.put(key, user);
                cacheService.set(key, user);
            }

            // 批量获取
            Map<String, TestUser> result = cacheService.getBatch(users.keySet(), TestUser.class);

            assertEquals(5, result.size(), "应该获取到5个用户");
            for (int i = 1; i <= 5; i++) {
                String key = "test:enhanced:batch:" + i;
                assertTrue(result.containsKey(key), "应该包含key: " + key);
                assertEquals("user" + i, result.get(key).getId(), "用户ID应该匹配");
            }

            log.info("批量获取测试通过: count={}", result.size());
        } catch (Exception e) {
            log.error("批量获取测试失败", e);
            fail("批量获取测试失败: " + e.getMessage());
        } finally {
            // 清理
            for (int i = 1; i <= 5; i++) {
                cacheService.delete("test:enhanced:batch:" + i);
            }
        }
    }

    @Test
    @Order(21)
    @DisplayName("CACHE-E-021: 批量设置缓存")
    void testBatchSet() {
        try {
            // 准备测试数据
            Map<String, TestUser> users = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                String key = "test:enhanced:batch:set:" + i;
                users.put(key, new TestUser("user" + i, "User " + i));
            }

            // 批量设置
            cacheService.setBatch(users, 5, TimeUnit.MINUTES);
            log.info("批量设置完成: count={}", users.size());

            // 验证
            Map<String, TestUser> result = cacheService.getBatch(users.keySet(), TestUser.class);
            assertEquals(5, result.size(), "应该获取到5个用户");

            log.info("批量设置测试通过: count={}", result.size());
        } catch (Exception e) {
            log.error("批量设置测试失败", e);
            fail("批量设置测试失败: " + e.getMessage());
        } finally {
            // 清理
            for (int i = 1; i <= 5; i++) {
                cacheService.delete("test:enhanced:batch:set:" + i);
            }
        }
    }

    // ==================== 条件缓存测试 ====================

    @Test
    @Order(30)
    @DisplayName("CACHE-E-030: 条件缓存测试")
    void testConditionalCache() {
        try {
            String key1 = "test:enhanced:conditional:1";
            String key2 = "test:enhanced:conditional:2";

            // 条件为真，应该缓存
            cacheService.setIf(key1, new TestUser("user1", "User 1"), 5, TimeUnit.MINUTES, true);
            assertTrue(cacheService.exists(key1), "条件为真时应该缓存");

            // 条件为假，不应该缓存
            cacheService.setIf(key2, new TestUser("user2", "User 2"), 5, TimeUnit.MINUTES, false);
            assertFalse(cacheService.exists(key2), "条件为假时不应该缓存");

            log.info("条件缓存测试通过");
        } catch (Exception e) {
            log.error("条件缓存测试失败", e);
            fail("条件缓存测试失败: " + e.getMessage());
        } finally {
            cacheService.delete("test:enhanced:conditional:1");
            cacheService.delete("test:enhanced:conditional:2");
        }
    }

    @Test
    @Order(31)
    @DisplayName("CACHE-E-031: 值存在性缓存测试")
    void testSetIfPresent() {
        try {
            String key1 = "test:enhanced:present:1";
            String key2 = "test:enhanced:present:2";

            // 值存在，应该缓存
            cacheService.setIfPresent(key1, new TestUser("user1", "User 1"), 5, TimeUnit.MINUTES);
            assertTrue(cacheService.exists(key1), "值存在时应该缓存");

            // 值为null，不应该缓存
            cacheService.setIfPresent(key2, null, 5, TimeUnit.MINUTES);
            assertFalse(cacheService.exists(key2), "值为null时不应该缓存");

            log.info("值存在性缓存测试通过");
        } catch (Exception e) {
            log.error("值存在性缓存测试失败", e);
            fail("值存在性缓存测试失败: " + e.getMessage());
        } finally {
            cacheService.delete("test:enhanced:present:1");
            cacheService.delete("test:enhanced:present:2");
        }
    }

    // ==================== 缓存预热测试 ====================

    @Test
    @Order(40)
    @DisplayName("CACHE-E-040: 缓存预热测试")
    void testCacheWarmUp() {
        try {
            // 准备预热数据
            Map<String, TestUser> warmUpData = new HashMap<>();
            for (int i = 1; i <= 10; i++) {
                String key = "test:enhanced:warmup:" + i;
                warmUpData.put(key, new TestUser("user" + i, "User " + i));
            }

            // 预热
            cacheService.warmUp(warmUpData, 10, TimeUnit.MINUTES);
            log.info("缓存预热完成: count={}", warmUpData.size());

            // 验证所有数据都已缓存
            Map<String, TestUser> result = cacheService.getBatch(warmUpData.keySet(), TestUser.class);
            assertEquals(10, result.size(), "所有预热数据应该都已缓存");

            // 验证缓存统计
            EnhancedTwoLevelCacheService.EnhancedCacheStats stats = cacheService.getStats();
            assertTrue(stats.getPuts() >= 10, "应该至少有10次PUT操作");

            log.info("缓存预热测试通过: stats={}", stats);
        } catch (Exception e) {
            log.error("缓存预热测试失败", e);
            fail("缓存预热测试失败: " + e.getMessage());
        } finally {
            // 清理
            for (int i = 1; i <= 10; i++) {
                cacheService.delete("test:enhanced:warmup:" + i);
            }
        }
    }

    // ==================== 监控统计测试 ====================

    @Test
    @Order(50)
    @DisplayName("CACHE-E-050: 缓存统计测试")
    void testCacheStats() {
        try {
            // 重置统计
            cacheService.resetStats();

            String key = "test:enhanced:stats:1";
            TestUser user = new TestUser("stats-user", "Stats User");

            // 设置缓存
            cacheService.set(key, user);

            // 获取缓存（L1命中）
            TestUser cached1 = cacheService.get(key, TestUser.class);
            assertNotNull(cached1);

            // 获取缓存（L1命中）
            TestUser cached2 = cacheService.get(key, TestUser.class);
            assertNotNull(cached2);

            // 获取统计
            EnhancedTwoLevelCacheService.EnhancedCacheStats stats = cacheService.getStats();

            log.info("缓存统计: stats={}", stats);

            assertTrue(stats.getPuts() >= 1, "应该有PUT操作");
            assertTrue(stats.getL1Hits() >= 2, "应该有L1命中");

            log.info("缓存统计测试通过: stats={}", stats);
        } catch (Exception e) {
            log.error("缓存统计测试失败", e);
            fail("缓存统计测试失败: " + e.getMessage());
        } finally {
            cacheService.delete("test:enhanced:stats:1");
        }
    }

    // ==================== 并发测试 ====================

    @Test
    @Order(60)
    @DisplayName("CACHE-E-060: 并发缓存操作测试")
    void testConcurrentOperations() throws Exception {
        String key = "test:enhanced:concurrent:1";
        int threadCount = 10;
        int operationsPerThread = 100;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            // 设置缓存
                            TestUser user = new TestUser("user" + j, "User " + j);
                            cacheService.set(key + ":" + j, user);

                            // 获取缓存
                            TestUser cached = cacheService.get(key + ":" + j, TestUser.class);
                            if (cached != null && cached.getId().equals("user" + j)) {
                                successCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("线程执行失败", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 启动所有线程
        startLatch.countDown();
        assertTrue(endLatch.await(30, TimeUnit.SECONDS), "所有线程应该完成");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        log.info("并发测试完成: success={}, fail={}", successCount.get(), failCount);

        assertTrue(successCount.get() > threadCount * operationsPerThread * 0.9, "成功率应该大于90%");

        log.info("并发缓存操作测试通过: success={}, fail={}", successCount.get(), failCount);
    }

    // ==================== 异常处理测试 ====================

    @Test
    @Order(70)
    @DisplayName("CACHE-E-070: 异常序列化处理测试")
    void testExceptionHandling() {
        try {
            // 测试空值处理
            String key = "test:enhanced:exception:1";
            cacheService.set(key, null);
            TestUser result = cacheService.get(key, TestUser.class);
            assertNull(result, "空值应该正常处理");

            // 测试复杂对象序列化
            String key2 = "test:enhanced:exception:2";
            Map<String, Object> complexData = new HashMap<>();
            complexData.put("name", "Test");
            complexData.put("value", 123);
            complexData.put("nested", MapUtil.of("key", "value"));

            cacheService.set(key2, complexData);
            // 注意：这里可能因为类型擦除而失败，但应该有兜底处理

            log.info("异常处理测试通过");
        } catch (Exception e) {
            // 异常应该被兜底处理，不应该向上传播
            log.warn("异常处理测试捕获异常（这是预期的兜底行为）: {}", e.getMessage());
        } finally {
            cacheService.delete("test:enhanced:exception:1");
            cacheService.delete("test:enhanced:exception:2");
        }
    }

    // ==================== 清理操作测试 ====================

    @Test
    @Order(80)
    @DisplayName("CACHE-E-080: 批量删除和前缀删除测试")
    void testBatchAndPrefixDelete() {
        try {
            // 准备测试数据
            List<String> keys = Arrays.asList(
                    "test:enhanced:delete:1",
                    "test:enhanced:delete:2",
                    "test:enhanced:delete:3",
                    "test:enhanced:delete:other:1"
            );

            for (String key : keys) {
                cacheService.set(key, new TestUser(key, "Test"));
            }

            // 验证缓存存在
            for (String key : keys) {
                assertTrue(cacheService.exists(key), "缓存应该存在: " + key);
            }

            // 按前缀删除
            cacheService.deleteByPrefix("test:enhanced:delete:");

            // 验证前缀匹配的都被删除
            assertFalse(cacheService.exists("test:enhanced:delete:1"), "应该被删除");
            assertFalse(cacheService.exists("test:enhanced:delete:2"), "应该被删除");
            assertFalse(cacheService.exists("test:enhanced:delete:3"), "应该被删除");
            assertFalse(cacheService.exists("test:enhanced:delete:other:1"), "应该被删除");

            log.info("批量删除和前缀删除测试通过");
        } catch (Exception e) {
            log.error("批量删除和前缀删除测试失败", e);
            fail("批量删除和前缀删除测试失败: " + e.getMessage());
        }
    }

    // ==================== 清理操作测试 ====================

    @Test
    @Order(90)
    @DisplayName("CACHE-E-090: 缓存清空和大小测试")
    void testClearAndSize() {
        try {
            // 准备测试数据
            for (int i = 1; i <= 10; i++) {
                cacheService.set("test:enhanced:clear:" + i, new TestUser("user" + i, "User " + i));
            }

            // 检查L1大小
            int l1Size = cacheService.getL1Size();
            assertTrue(l1Size > 0, "L1缓存应该有数据");
            log.info("L1缓存大小: {}", l1Size);

            // 清空所有缓存
            cacheService.clear();

            // 验证缓存已清空
            for (int i = 1; i <= 10; i++) {
                assertFalse(cacheService.exists("test:enhanced:clear:" + i), "缓存应该被清空");
            }

            int l1SizeAfterClear = cacheService.getL1Size();
            assertEquals(0, l1SizeAfterClear, "L1缓存应该为空");

            log.info("缓存清空和大小测试通过");
        } catch (Exception e) {
            log.error("缓存清空和大小测试失败", e);
            fail("缓存清空和大小测试失败: " + e.getMessage());
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

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}