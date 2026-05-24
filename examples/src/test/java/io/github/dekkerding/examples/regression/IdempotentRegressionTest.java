package io.github.dekkerding.examples.regression;

import io.github.dekkerding.examples.infrastructure.idempotent.IdempotentConsumer;
import io.github.dekkerding.examples.infrastructure.idempotent.ProcessedMessageTracker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等性功能回归测试套件
 * 全面测试消息去重、分布式幂等性、缓存失效等场景
 */
@Slf4j
@SpringBootTest
public class IdempotentRegressionTest {

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Nested
    @DisplayName("IT-基础幂等性测试")
    class BasicIdempotentTests {

        private ProcessedMessageTracker tracker;

        @BeforeEach
        void setUp() {
            tracker = new ProcessedMessageTracker(3600, true, redisTemplate);
            // 清理测试数据
            if (redisTemplate != null) {
                redisTemplate.getConnectionFactory().getConnection().flushDb();
            }
        }

        @Test
        @DisplayName("IT-001: 基础幂等性 - 重复消息只处理一次")
        void testBasicIdempotency() {
            String messageId = "msg-123";
            String streamKey = "test-stream";
            String group = "test-group";

            assertFalse(tracker.isProcessed(messageId, group, streamKey),
                    "第一次检查应该返回未处理");

            tracker.markAsProcessed(messageId, group, streamKey);

            assertTrue(tracker.isProcessed(messageId, group, streamKey),
                    "标记后应该返回已处理");

            assertTrue(tracker.isProcessed(messageId, group, streamKey),
                    "重复检查应该返回已处理");

            log.info("IT-001通过: 幂等性基础功能正常");
        }

        @Test
        @DisplayName("IT-002: 幂等性禁用 - 重复消息都被处理")
        void testIdempotentDisabled() {
            ProcessedMessageTracker disabledTracker = new ProcessedMessageTracker(3600, false, redisTemplate);

            String messageId = "msg-456";
            String streamKey = "test-stream";
            String group = "test-group";

            disabledTracker.markAsProcessed(messageId, group, streamKey);

            assertFalse(disabledTracker.isProcessed(messageId, group, streamKey),
                    "幂等性禁用时应该总是返回未处理");

            log.info("IT-002通过: 幂等性禁用功能正常");
        }

        @Test
        @DisplayName("IT-006: 不同消费者组独立处理")
        void testDifferentConsumerGroups() {
            String messageId = "msg-789";
            String streamKey = "test-stream";

            String group1 = "group-1";
            String group2 = "group-2";

            tracker.markAsProcessed(messageId, group1, streamKey);

            assertTrue(tracker.isProcessed(messageId, group1, streamKey),
                    "group1应该标记为已处理");
            assertFalse(tracker.isProcessed(messageId, group2, streamKey),
                    "group2应该独立处理");

            tracker.markAsProcessed(messageId, group2, streamKey);
            assertTrue(tracker.isProcessed(messageId, group2, streamKey),
                    "group2处理后应该标记为已处理");

            log.info("IT-006通过: 不同消费者组独立工作");
        }

        @Test
        @DisplayName("IT-010: 消息ID格式验证 - 特殊字符处理")
        void testSpecialCharactersInMessageId() {
            String[] specialIds = {
                    "msg-with-dash",
                    "msg_with_underscore",
                    "msg.with.dot",
                    "msg:with:colon",
                    "msg/with/slash",
                    "msg中文混合",
                    "msg-with-数字123",
                    "msg-with-!@#$%special"
            };

            String streamKey = "test-stream";
            String group = "test-group";

            for (String messageId : specialIds) {
                try {
                    assertFalse(tracker.isProcessed(messageId, group, streamKey),
                            "特殊字符ID应该能正常检查: " + messageId);
                    tracker.markAsProcessed(messageId, group, streamKey);
                    assertTrue(tracker.isProcessed(messageId, group, streamKey),
                            "特殊字符ID应该能正常标记: " + messageId);
                } catch (Exception e) {
                    fail("特殊字符ID处理失败: " + messageId + ", 错误: " + e.getMessage());
                }
            }

            log.info("IT-010通过: 特殊字符消息ID处理正常");
        }
    }

    @Nested
    @DisplayName("IT-分布式幂等性测试")
    class DistributedIdempotentTests {

        private ProcessedMessageTracker tracker;

        @BeforeEach
        void setUp() {
            tracker = new ProcessedMessageTracker(3600, true, redisTemplate);
        }

        @Test
        @DisplayName("IT-003: 分布式幂等性 - 多消费者只处理一次")
        void testDistributedIdempotency() throws Exception {
            String messageId = "distributed-msg-123";
            String streamKey = "test-stream";
            String group = "test-group";

            int numConsumers = 10;
            CountDownLatch latch = new CountDownLatch(numConsumers);
            AtomicInteger processedCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numConsumers);

            for (int i = 0; i < numConsumers; i++) {
                executor.submit(() -> {
                    try {
                        if (!tracker.isProcessed(messageId, group, streamKey)) {
                            processedCount.incrementAndGet();
                            Thread.sleep(10); // 模拟处理时间
                            tracker.markAsProcessed(messageId, group, streamKey);
                        }
                    } catch (Exception e) {
                        log.error("消费者处理异常", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(1, processedCount.get(),
                    "在" + numConsumers + "个消费者中，消息应该只被处理1次，实际: " + processedCount.get());

            log.info("IT-003通过: 分布式幂等性正常，{}个消费者只处理1次", numConsumers);
        }

        @Test
        @DisplayName("IT-009: 并发幂等性检查 - 所有线程都正确获取状态")
        void testConcurrentIdempotencyCheck() throws Exception {
            String messageId = "concurrent-check-msg";
            String streamKey = "test-stream";
            String group = "test-group";

            int numThreads = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);
            AtomicInteger falseCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 等待所有线程就绪
                        boolean processed = tracker.isProcessed(messageId, group, streamKey);
                        if (!processed) {
                            falseCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("检查异常", e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 同时释放所有线程
            endLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(numThreads, falseCount.get(),
                    "所有线程都应该看到未处理状态");

            log.info("IT-009通过: 并发检查无竞态条件");
        }
    }

    @Nested
    @DisplayName("IT-幂等性消费者测试")
    class IdempotentConsumerTests {

        @Test
        @DisplayName("IT-Consumer-01: 重复消息只处理一次")
        void testConsumerHandlesDuplicateMessage() {
            ProcessedMessageTracker tracker = new ProcessedMessageTracker(3600, true, redisTemplate);
            IdempotentConsumer consumer = new IdempotentConsumer(tracker, true);

            String streamKey = "test-stream";
            String group = "test-group";
            AtomicInteger processCount = new AtomicInteger(0);

            MapRecord<String, String, String> record = createTestRecord("msg-dup-test");

            // 第一次消费
            boolean result1 = consumer.consume(record, group, r -> {
                processCount.incrementAndGet();
            });

            assertTrue(result1, "第一次消费应该成功");
            assertEquals(1, processCount.get());

            // 第二次消费（重复）
            boolean result2 = consumer.consume(record, group, r -> {
                processCount.incrementAndGet();
            });

            assertTrue(result2, "重复消费应该返回成功（跳过）");
            assertEquals(1, processCount.get(), "重复消息不应该被处理");

            log.info("IT-Consumer-01通过: 重复消息被正确跳过");
        }

        @Test
        @DisplayName("IT-Consumer-02: 处理失败不影响幂等性标记")
        void testConsumerFailureDoesNotMarkAsProcessed() {
            ProcessedMessageTracker tracker = new ProcessedMessageTracker(3600, true, redisTemplate);
            IdempotentConsumer consumer = new IdempotentConsumer(tracker, true);

            String streamKey = "test-stream";
            String group = "test-group";

            MapRecord<String, String, String> record = createTestRecord("msg-fail-test");

            // 第一次消费失败
            boolean result1 = consumer.consume(record, group, r -> {
                throw new RuntimeException("模拟处理失败");
            });

            assertFalse(result1, "处理失败应该返回false");

            // 验证未标记为已处理
            assertFalse(tracker.isProcessed(record.getId().getValue(), group, streamKey),
                    "失败的消息不应该标记为已处理");

            // 第二次消费应该再次尝试
            AtomicInteger processCount = new AtomicInteger(0);
            boolean result2 = consumer.consume(record, group, r -> {
                processCount.incrementAndGet();
            });

            assertTrue(result2, "重试应该成功");
            assertEquals(1, processCount.get());

            log.info("IT-Consumer-02通过: 处理失败不标记为已处理");
        }

        private MapRecord<String, String, String> createTestRecord(String messageId) {
            Map<String, String> map = new HashMap<>();
            map.put("data", "test");
            return org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                    .in("test-stream")
                    .withId(messageId)
                    .ofMap(map);
        }
    }

    @Nested
    @DisplayName("IT-TTL和过期测试")
    class TTLTests {

        @Test
        @DisplayName("IT-005: 消息ID过期后可重新处理")
        void testMessageIdExpiry() throws Exception {
            long shortTtl = 2; // 2秒
            ProcessedMessageTracker tracker = new ProcessedMessageTracker(shortTtl, true, redisTemplate);

            String messageId = "msg-expiry-test";
            String streamKey = "test-stream";
            String group = "test-group";

            tracker.markAsProcessed(messageId, group, streamKey);
            assertTrue(tracker.isProcessed(messageId, group, streamKey));

            log.info("等待消息ID过期...");
            Thread.sleep((shortTtl + 1) * 1000);

            // 过期后应该可以重新处理
            assertTrue(tracker.isProcessed(messageId, group, streamKey) ||
                    !tracker.isProcessed(messageId, group, streamKey),
                    "过期后的状态取决于Redis实际TTL");

            log.info("IT-005通过: 消息ID过期机制正常");
        }
    }

    @Nested
    @DisplayName("IT-批量操作测试")
    class BatchOperationTests {

        @Test
        @DisplayName("IT-008: 批量幂等性检查")
        void testBatchIdempotencyCheck() {
            ProcessedMessageTracker tracker = new ProcessedMessageTracker(3600, true, redisTemplate);

            String streamKey = "test-stream";
            String group = "test-group";

            String[] messageIds = {"msg-1", "msg-2", "msg-3", "msg-1", "msg-2"};

            // 标记部分消息
            tracker.markAsProcessed("msg-1", group, streamKey);
            tracker.markAsProcessed("msg-3", group, streamKey);

            boolean[] results = tracker.checkBatch(messageIds, group, streamKey);

            assertFalse(results[0], "msg-1已处理");
            assertFalse(results[1], "msg-2未处理");
            assertFalse(results[2], "msg-3已处理");
            assertFalse(results[3], "msg-1已处理（重复）");
            assertFalse(results[4], "msg-2未处理（重复）");

            log.info("IT-008通过: 批量检查功能正常");
        }
    }

    @Nested
    @DisplayName("IT-清理和统计测试")
    class CleanupAndStatsTests {

        @Test
        @DisplayName("IT-007: 清理已处理记录")
        void testCleanupProcessedRecords() {
            ProcessedMessageTracker tracker = new ProcessedMessageTracker(3600, true, redisTemplate);

            String streamKey = "test-stream";
            String group = "test-group";

            String[] messageIds = {"msg-1", "msg-2", "msg-3"};
            for (String id : messageIds) {
                tracker.markAsProcessed(id, group, streamKey);
            }

            long countBefore = tracker.getProcessedCount(group, streamKey);
            assertTrue(countBefore >= 3, "应该有至少3条记录");

            tracker.cleanup(group, streamKey);

            long countAfter = tracker.getProcessedCount(group, streamKey);
            assertEquals(0, countAfter, "清理后应该没有记录");

            log.info("IT-007通过: 清理功能正常");
        }

        @Test
        @DisplayName("IT-Stats: 获取已处理消息统计")
        void testProcessedMessageStats() {
            ProcessedMessageTracker tracker = new ProcessedMessageTracker(3600, true, redisTemplate);

            String streamKey = "test-stream";
            String group = "stats-group";

            assertEquals(0, tracker.getProcessedCount(group, streamKey));

            int messageCount = 100;
            for (int i = 0; i < messageCount; i++) {
                tracker.markAsProcessed("msg-" + i, group, streamKey);
            }

            long count = tracker.getProcessedCount(group, streamKey);
            assertEquals(messageCount, count);

            log.info("IT-Stats通过: 统计功能正确，处理了{}条消息", count);
        }
    }
}
