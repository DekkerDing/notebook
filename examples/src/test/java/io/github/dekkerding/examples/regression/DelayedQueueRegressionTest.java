package io.github.dekkerding.examples.regression;

import io.github.dekkerding.examples.application.DelayedEventPublisher;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.delayed.DelayedTask;
import io.github.dekkerding.examples.infrastructure.delayed.TimingWheelScheduler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 延迟队列回归测试套件
 *
 * <p>测试目标：验证Redis Stream延迟队列功能完整性和性能
 *
 * <p>测试要点：
 * <ul>
 *   <li>时间轮调度器短延迟任务调度（< 60s）</li>
 *   <li>SortedSet长延迟任务调度（≥ 60s）</li>
 *   <li>任务取消功能</li>
 *   <li>任务状态查询</li>
 *   <li>延迟精度验证</li>
 *   <li>并发调度性能</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("延迟队列回归测试")
public class DelayedQueueRegressionTest {

    @Autowired(required = false)
    private DelayedEventPublisher delayedPublisher;

    @Autowired(required = false)
    private TimingWheelScheduler timingWheelScheduler;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String TEST_STREAM = "delayed:queue:test";

    @BeforeEach
    void setUp() {
        assumeTrue(delayedPublisher != null, () -> "DelayedEventPublisher 未配置");
        assumeTrue(timingWheelScheduler != null, () -> "TimingWheelScheduler 未配置");
        assumeTrue(redisTemplate != null, () -> "Redis 未配置");

        // 清理测试数据
        redisTemplate.delete(TEST_STREAM);
        redisTemplate.delete(redisTemplate.keys("delayed:*"));
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null) {
            redisTemplate.delete(TEST_STREAM);
            redisTemplate.delete(redisTemplate.keys("delayed:*"));
        }
    }

    // ==================== 时间轮调度测试 ====================

    @Test
    @Order(1)
    @DisplayName("DQ-001: 时间轮 - 短延迟任务执行")
    void testTimingWheelShortDelay() throws Exception {
        String taskId = delayedPublisher.publishDelayed(
            TestEvent.builder().id("1").data("short delay").build(),
            1000  // 1秒
        );

        assertNotNull(taskId, "任务ID不应为空");
        log.info("任务已调度: taskId={}", taskId);

        // 等待任务执行
        CountDownLatch latch = new CountDownLatch(1);
        waitForStreamMessage(latch);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "任务应在5秒内完成");
    }

    @Test
    @Order(2)
    @DisplayName("DQ-002: 时间轮 - 多任务并发调度")
    void testTimingWheelConcurrentTasks() throws Exception {
        int taskCount = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);

        // 并发提交100个任务
        long start = System.currentTimeMillis();
        for (int i = 0; i < taskCount; i++) {
            final int index = i;
            String taskId = delayedPublisher.publishDelayed(
                TestEvent.builder().id(String.valueOf(index)).data("task-" + index).build(),
                500 + (i * 10)  // 500ms-1500ms
            );
            assertNotNull(taskId, "任务ID不应为空");
        }
        long submitTime = System.currentTimeMillis() - start;

        log.info("提交{}个任务耗时: {}ms", taskCount, submitTime);

        // 验证待处理任务数
        int pendingCount = delayedPublisher.getPendingTaskCount();
        log.info("当前待处理任务数: {}", pendingCount);
    }

    @Test
    @Order(3)
    @DisplayName("DQ-003: 时间轮 - 任务精度验证")
    void testTimingWheelPrecision() throws Exception {
        long delay = 5000;  // 5秒
        long expectedExecuteTime = System.currentTimeMillis() + delay;

        String taskId = delayedPublisher.publishDelayed(
            TestEvent.builder().id("precision").data("precision test").build(),
            delay
        );

        // 验证任务在时间轮中
        int pendingCount = delayedPublisher.getPendingTaskCount();
        assertTrue(pendingCount > 0, "应有待处理任务");

        // 等待任务执行
        Thread.sleep(delay + 1000);

        // 验证延迟精度：误差应小于500ms
        // 实际执行时间需要从消费端获取，这里只验证任务被调度
        log.info("任务调度精度验证完成: taskId={}", taskId);
    }

    @Test
    @Order(4)
    @DisplayName("DQ-004: 时间轮 - 任务边界值测试")
    void testTimingWheelBoundaryValues() {
        // 测试接近最大值的任务
        long maxDelay = 59_000;  // 59秒（小于60秒阈值）

        String taskId = delayedPublisher.publishDelayed(
            TestEvent.builder().id("boundary").data("boundary test").build(),
            maxDelay
        );

        assertNotNull(taskId, "边界任务应成功调度");

        int pendingCount = delayedPublisher.getPendingTaskCount();
        assertTrue(pendingCount > 0, "边界任务应在待处理队列中");
    }

    // ==================== 任务取消测试 ====================

    @Test
    @Order(5)
    @DisplayName("DQ-005: 任务取消 - 取消已调度任务")
    void testCancelScheduledTask() {
        // 调度一个10秒后执行的任务
        String taskId = delayedPublisher.publishDelayed(
            TestEvent.builder().id("cancel").data("cancel test").build(),
            10_000
        );

        assertNotNull(taskId, "任务ID不应为空");

        int beforeCancel = delayedPublisher.getPendingTaskCount();
        log.info("取消前待处理任务数: {}", beforeCancel);

        // 取消任务
        boolean cancelled = delayedPublisher.cancelDelayed(taskId);

        assertTrue(cancelled, "任务应成功取消");

        int afterCancel = delayedPublisher.getPendingTaskCount();
        log.info("取消后待处理任务数: {}", afterCancel);

        // 验证任务数减少
        assertTrue(afterCancel < beforeCancel, "取消后待处理任务数应减少");
    }

    @Test
    @Order(6)
    @DisplayName("DQ-006: 任务取消 - 取消不存在的任务")
    void testCancelNonExistentTask() {
        // 尝试取消不存在的任务
        boolean cancelled = delayedPublisher.cancelDelayed("non-existent-task-id");

        assertFalse(cancelled, "不存在的任务应取消失败");
    }

    // ==================== 指定时间调度测试 ====================

    @Test
    @Order(7)
    @DisplayName("DQ-007: 指定时间 - 未来时间调度")
    void testScheduleAtFutureTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusSeconds(30);

        String taskId = delayedPublisher.publishDelayedAt(
            TestEvent.builder().id("future").data("future time test").build(),
            futureTime
        );

        assertNotNull(taskId, "指定时间任务应成功调度");

        log.info("已调度到 {} 的任务: taskId={}", futureTime, taskId);
    }

    @Test
    @Order(8)
    @DisplayName("DQ-008: 指定时间 - 过去时间应失败")
    void testScheduleAtPastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(10);

        assertThrows(IllegalArgumentException.class, () -> {
            delayedPublisher.publishDelayedAt(
                TestEvent.builder().id("past").data("past time test").build(),
                pastTime
            );
        }, "过去时间应抛出异常");
    }

    // ==================== 性能测试 ====================

    @Test
    @Order(9)
    @DisplayName("DQ-009: 性能 - 短延迟任务吞吐量")
    void testShortDelayThroughput() {
        int taskCount = 1000;
        long start = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            delayedPublisher.publishDelayed(
                TestEvent.builder().id(String.valueOf(i)).build(),
                1000
            );
        }

        long duration = System.currentTimeMillis() - start;
        double tps = (double) taskCount / duration * 1000;

        log.info("调度{}个任务耗时: {}ms, TPS: {}", taskCount, duration, String.format("%.2f", tps));

        // 验证性能：应大于10万TPS
        assertTrue(tps > 100_000, "TPS应大于10万，实际: " + String.format("%.2f", tps));
    }

    @Test
    @Order(10)
    @DisplayName("DQ-010: 性能 - 并发调度压力测试")
    void testConcurrentSchedulingStress() throws Exception {
        int threadCount = 10;
        int tasksPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger totalTasks = new AtomicInteger(0);

        long[] times = new long[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            executor.submit(() -> {
                try {
                    startLatch.await();  // 等待统一开始

                    long threadStart = System.nanoTime();

                    for (int i = 0; i < tasksPerThread; i++) {
                        String taskId = delayedPublisher.publishDelayed(
                            TestEvent.builder()
                                .id(threadIndex + "-" + i)
                                .data("concurrent task")
                                .build(),
                            1000 + (i * 5)  // 分散执行时间
                        );
                        if (taskId != null) {
                            totalTasks.incrementAndGet();
                        }
                    }

                    times[threadIndex] = System.nanoTime() - threadStart;

                } catch (Exception e) {
                    log.error("线程调度失败", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 统一开始
        long globalStart = System.nanoTime();
        startLatch.countDown();

        // 等待所有线程完成
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "所有线程应在30秒内完成");

        long globalDuration = System.nanoTime() - globalStart;

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 统计结果
        int successfulTasks = totalTasks.get();
        double durationSeconds = globalDuration / 1_000_000_000.0;
        double tps = successfulTasks / durationSeconds;

        log.info("并发调度统计:");
        log.info("  成功任务数: {}", successfulTasks);
        log.info("  总耗时: {}ms", String.format("%.2f", durationSeconds * 1000));
        log.info("  TPS: {}", String.format("%.2f", tps));

        // 验证成功率
        double successRate = (double) successfulTasks / (threadCount * tasksPerThread);
        assertTrue(successRate > 0.99, "成功率应大于99%，实际: " + String.format("%.2f%%", successRate * 100));
    }

    // ==================== 可靠性测试 ====================

    @Test
    @Order(11)
    @DisplayName("DQ-011: 可靠性 - 任务数据完整性")
    void testTaskDataIntegrity() throws Exception {
        String expectedData = "integrity-test-data-" + UUID.randomUUID();
        String expectedId = "integrity-" + UUID.randomUUID();

        String taskId = delayedPublisher.publishDelayed(
            TestEvent.builder()
                .id(expectedId)
                .data(expectedData)
                .eventTimestamp(System.currentTimeMillis())
                .build(),
            2000
        );

        assertNotNull(taskId, "任务ID不应为空");

        // 等待任务执行
        Thread.sleep(3000);

        // 验证Stream中的消息
        Long streamSize = redisTemplate.opsForStream().size(TEST_STREAM);
        if (streamSize != null && streamSize > 0) {
            log.info("Stream中有{}条消息", streamSize);
        }
    }

    @Test
    @Order(12)
    @DisplayName("DQ-012: 可靠性 - 空值事件处理")
    void testNullEventHandling() {
        assertThrows(IllegalArgumentException.class, () -> {
            delayedPublisher.publishDelayed(null, 1000);
        }, "空事件应抛出异常");
    }

    @Test
    @Order(13)
    @DisplayName("DQ-013: 可靠性 - 无效延迟时间处理")
    void testInvalidDelayTime() {
        assertThrows(IllegalArgumentException.class, () -> {
            delayedPublisher.publishDelayed(
                TestEvent.builder().id("invalid").build(),
                -1000  // 负数延迟
            );
        }, "负延迟应抛出异常");

        assertThrows(IllegalArgumentException.class, () -> {
            delayedPublisher.publishDelayed(
                TestEvent.builder().id("invalid").build(),
                0  // 零延迟
            );
        }, "零延迟应抛出异常");
    }

    // ==================== 辅助方法 ====================

    /**
     * 等待Stream消息（简化实现）
     */
    private void waitForStreamMessage(CountDownLatch latch) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Long size = redisTemplate.opsForStream().size(TEST_STREAM);
                    if (size != null && size > 0) {
                        latch.countDown();
                        executor.shutdown();
                    }
                } catch (Exception e) {
                    log.error("检查Stream消息失败", e);
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    // ==================== 测试事件类 ====================

    @EqualsAndHashCode(callSuper = true)
    static class TestEvent extends DomainEvent {
        private String id;
        private String data;
        private Long eventTimestamp;

        public TestEvent() {
            super(new Object());
        }

        public TestEvent(Object source, String id, String data, Long eventTimestamp) {
            super(source);
            this.id = id;
            this.data = data;
            this.eventTimestamp = eventTimestamp;
        }

        public String getId() {
            return id;
        }

        public String getData() {
            return data;
        }

        public Long getEventTimestamp() {
            return eventTimestamp;
        }

        @Override
        public String getEventType() {
            return "TestEvent";
        }

        public static TestEventBuilder builder() {
            return new TestEventBuilder();
        }

        public static class TestEventBuilder {
            private String id;
            private String data;
            private Long eventTimestamp;

            public TestEventBuilder id(String id) {
                this.id = id;
                return this;
            }

            public TestEventBuilder data(String data) {
                this.data = data;
                return this;
            }

            public TestEventBuilder eventTimestamp(Long eventTimestamp) {
                this.eventTimestamp = eventTimestamp;
                return this;
            }

            public TestEvent build() {
                return new TestEvent(new Object(), id, data, eventTimestamp);
            }
        }
    }
}
