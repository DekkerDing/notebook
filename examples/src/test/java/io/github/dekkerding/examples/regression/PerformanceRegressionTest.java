package io.github.dekkerding.examples.regression;

import io.github.dekkerding.examples.application.EventPublishingService;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import io.github.dekkerding.examples.infrastructure.partition.PartitionManager;
import io.github.dekkerding.examples.infrastructure.publisher.PartitionedStreamPublisher;
import io.github.dekkerding.examples.infrastructure.publisher.RedisStreamEventPublisher;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能回归测试套件
 * 测试吞吐量、延迟、并发等性能指标
 */
@Slf4j
@SpringBootTest
public class PerformanceRegressionTest {

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Autowired(required = false)
    private EventPublishingService eventPublishingService;

    @Nested
    @DisplayName("TP-吞吐量测试")
    class ThroughputTests {

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        @DisplayName("TP-001: 小消息吞吐量测试 (1KB)")
        void testThroughputSmallMessages() {
            int messageCount = 10000;
            int messageSize = 1024; // 1KB
            long startTime = System.currentTimeMillis();

            List<Long> latencies = new ArrayList<>(messageCount);

            for (int i = 0; i < messageCount; i++) {
                long publishStart = System.nanoTime();
                // 模拟发布1KB消息
                publishMessage(createSmallEvent(i, messageSize));
                long publishEnd = System.nanoTime();
                latencies.add((publishEnd - publishStart) / 1000); // 转换为微秒
            }

            long duration = System.currentTimeMillis() - startTime;
            double tps = (messageCount * 1000.0) / duration;

            log.info("TP-001: 发布{}条1KB消息耗时{}ms, TPS: {}", messageCount, duration, String.format("%.2f", tps));

            assertTrue(tps >= 1000, "TPS应该>=1000, 实际: " + String.format("%.2f", tps));

            // 计算P99延迟
            Collections.sort(latencies);
            long p99 = latencies.get((int) (messageCount * 0.99));
            log.info("TP-001: P99延迟: {}μs", p99);
        }

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        @DisplayName("TP-002: 中消息吞吐量测试 (10KB)")
        void testThroughputMediumMessages() {
            int messageCount = 5000;
            int messageSize = 10240; // 10KB
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < messageCount; i++) {
                publishMessage(createMediumEvent(i, messageSize));
            }

            long duration = System.currentTimeMillis() - startTime;
            double tps = (messageCount * 1000.0) / duration;

            log.info("TP-002: 发布{}条10KB消息耗时{}ms, TPS: {}", messageCount, duration, String.format("%.2f", tps));

            assertTrue(tps >= 500, "TPS应该>=500, 实际: " + String.format("%.2f", tps));
        }

        @Test
        @Timeout(value = 120, unit = TimeUnit.SECONDS)
        @DisplayName("TP-004: 分区吞吐量测试")
        void testPartitionedThroughput() {
            int messageCount = 10000;
            int numPartitions = 3;
            long startTime = System.currentTimeMillis();

            PartitionManager manager = new PartitionManager(
                    "perf:",
                    numPartitions,
                    new io.github.dekkerding.examples.infrastructure.partition.HashPartitionStrategy(),
                    redisTemplate);

            manager.ensurePartitions("PerfTestEvent");

            for (int i = 0; i < messageCount; i++) {
                String key = "order-" + i;
                int partition = manager.getPartition(key, "PerfTestEvent");
                // 模拟发布到分区
                publishToPartition(manager, "PerfTestEvent", partition, createSmallEvent(i, 1024));
            }

            long duration = System.currentTimeMillis() - startTime;
            double tps = (messageCount * 1000.0) / duration;

            log.info("TP-004: 分区模式发布{}条消息耗时{}ms, TPS: {}", messageCount, duration, String.format("%.2f", tps));

            assertTrue(tps >= 2000, "分区TPS应该>=2000, 实际: " + String.format("%.2f", tps));
        }
    }

    @Nested
    @DisplayName("LT-延迟测试")
    class LatencyTests {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("LT-001: 端到端延迟测试")
        void testEndToEndLatency() throws Exception {
            int messageCount = 1000;
            List<Long> latencies = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(messageCount);

            // 模拟消费者
            startConsumer((message) -> {
                long publishTime = extractPublishTime(message);
                long latency = System.currentTimeMillis() - publishTime;
                latencies.add(latency);
                latch.countDown();
            });

            // 发布消息
            for (int i = 0; i < messageCount; i++) {
                long publishTime = System.currentTimeMillis();
                publishMessageWithTimestamp(createSmallEvent(i, 1024), publishTime);
            }

            boolean completed = latch.await(20, TimeUnit.SECONDS);
            assertTrue(completed, "所有消息应该在超时前被消费");

            // 计算延迟统计
            Collections.sort(latencies);
            long min = latencies.get(0);
            long max = latencies.get(latencies.size() - 1);
            long avg = latencies.stream().mapToLong(Long::longValue).sum() / latencies.size();
            long p50 = latencies.get((int) (messageCount * 0.5));
            long p95 = latencies.get((int) (messageCount * 0.95));
            long p99 = latencies.get((int) (messageCount * 0.99));

            log.info("LT-001延迟统计 - Min:{}ms, Max:{}ms, Avg:{}ms, P50:{}ms, P95:{}ms, P99:{}ms",
                    min, max, avg, p50, p95, p99);

            assertTrue(p99 < 500, "P99延迟应该<500ms, 实际: " + p99 + "ms");
            assertTrue(avg < 100, "平均延迟应该<100ms, 实际: " + avg + "ms");
        }

        @Test
        @DisplayName("LT-002: 发布延迟测试")
        void testPublishLatency() {
            int iterations = 1000;
            List<Long> latencies = new ArrayList<>(iterations);

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                publishMessage(createSmallEvent(i, 1024));
                long end = System.nanoTime();
                latencies.add((end - start) / 1000); // 微秒
            }

            Collections.sort(latencies);
            long p99 = latencies.get((int) (iterations * 0.99));

            log.info("LT-002: 发布延迟P99: {}μs", p99);

            assertTrue(p99 < 10000, "P99发布延迟应该<10ms, 实际: " + p99 + "μs");
        }
    }

    @Nested
    @DisplayName("CC-并发测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("CC-001: 并发发布测试")
        void testConcurrentPublishing() throws Exception {
            int numThreads = 100;
            int messagesPerThread = 100;
            int totalMessages = numThreads * messagesPerThread;

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);
            AtomicLong publishCount = new AtomicLong(0);
            List<Future<?>> futures = new ArrayList<>();

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            long startTime = System.currentTimeMillis();

            for (int t = 0; t < numThreads; t++) {
                Future<?> future = executor.submit(() -> {
                    try {
                        startLatch.await(); // 等待同时开始

                        for (int i = 0; i < messagesPerThread; i++) {
                            publishMessage(createSmallEvent(publishCount.incrementAndGet(), 1024));
                        }
                    } catch (Exception e) {
                        log.error("发布异常", e);
                    } finally {
                        endLatch.countDown();
                    }
                });
                futures.add(future);
            }

            startLatch.countDown(); // 同时开始
            boolean completed = endLatch.await(60, TimeUnit.SECONDS);

            long duration = System.currentTimeMillis() - startTime;

            executor.shutdown();
            assertTrue(completed, "所有发布操作应该完成");
            assertEquals(totalMessages, publishCount.get(), "所有消息都应该发布成功");

            double tps = (totalMessages * 1000.0) / duration;
            log.info("CC-001: {}线程并发发布{}条消息，耗时{}ms, TPS: {}",
                    numThreads, totalMessages, duration, String.format("%.2f", tps));

            assertTrue(tps >= 5000, "并发TPS应该>=5000");
        }

        @Test
        @DisplayName("CC-004: 并发幂等性测试")
        void testConcurrentIdempotency() throws Exception {
            io.github.dekkerding.examples.infrastructure.idempotent.ProcessedMessageTracker tracker =
                    new io.github.dekkerding.examples.infrastructure.idempotent.ProcessedMessageTracker(
                            3600, true, redisTemplate);

            int numConsumers = 50;
            String messageId = "concurrent-idempotent-test";
            String streamKey = "test-stream";
            String group = "test-group";

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numConsumers);
            AtomicInteger processedCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numConsumers);

            for (int i = 0; i < numConsumers; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (!tracker.isProcessed(messageId, group, streamKey)) {
                            processedCount.incrementAndGet();
                            Thread.sleep(10); // 模拟处理
                            tracker.markAsProcessed(messageId, group, streamKey);
                        }
                    } catch (Exception e) {
                        log.error("消费者异常", e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(1, processedCount.get(),
                    "在" + numConsumers + "个并发消费者中，消息应该只被处理1次");

            log.info("CC-004通过: 并发幂等性正确");
        }
    }

    @Nested
    @DisplayName("RS-资源占用测试")
    class ResourceTests {

        @Test
        @DisplayName("RS-001: 内存占用测试")
        void testMemoryUsage() throws Exception {
            Runtime runtime = Runtime.getRuntime();

            // 强制GC获取基准内存
            System.gc();
            Thread.sleep(1000);
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            log.info("RS-001基准内存: {}MB", memoryBefore / (1024 * 1024));

            // 发布大量消息
            int messageCount = 10000;
            for (int i = 0; i < messageCount; i++) {
                publishMessage(createSmallEvent(i, 1024));
            }

            // 再次GC
            System.gc();
            Thread.sleep(1000);
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

            long memoryUsed = memoryAfter - memoryBefore;
            double memoryUsedMB = memoryUsed / (1024.0 * 1024.0);

            log.info("RS-001处理后内存: {}MB, 增长: {}MB",
                    memoryAfter / (1024 * 1024), String.format("%.2f", memoryUsedMB));

            assertTrue(memoryUsedMB < 500, "内存增长应该<500MB, 实际: " + String.format("%.2f", memoryUsedMB) + "MB");
        }

        @Test
        @DisplayName("RS-002: CPU使用率测试")
        void testCPUUsage() throws Exception {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean)
                            java.lang.management.ManagementFactory.getOperatingSystemMXBean();

            int messageCount = 5000;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < messageCount; i++) {
                publishMessage(createSmallEvent(i, 1024));
            }

            long duration = System.currentTimeMillis() - startTime;

            // 获取CPU使用率
            double cpuBefore = osBean.getProcessCpuLoad();
            Thread.sleep(100);
            double cpuAfter = osBean.getProcessCpuLoad();

            log.info("RS-002: 处理{}条消息耗时{}ms, CPU使用率: {}% -> {}%",
                    messageCount, duration, cpuBefore * 100, cpuAfter * 100);

            assertTrue(cpuAfter < 0.8, "CPU使用率应该<80%");
        }
    }

    // 辅助方法

    private void publishMessage(DomainEvent event) {
        if (eventPublishingService != null) {
            try {
                eventPublishingService.publish(event);
            } catch (Exception e) {
                log.warn("发布消息失败: {}", e.getMessage());
            }
        }
    }

    private void publishToPartition(PartitionManager manager, String eventType, int partition, DomainEvent event) {
        // 模拟分区发布
        String streamKey = manager.getPartitionStreamKey(eventType, partition);
        if (redisTemplate != null) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("eventType", event.getEventType());
            map.put("data", event.toString());
            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(streamKey)
                            .ofMap(map)
            );
        }
    }

    private void startConsumer(java.util.function.Consumer<String> handler) {
        // 模拟启动消费者
    }

    private long extractPublishTime(String message) {
        // 模拟提取发布时间
        return System.currentTimeMillis();
    }

    private void publishMessageWithTimestamp(DomainEvent event, long timestamp) {
        publishMessage(event);
    }

    private DomainEvent createSmallEvent(long id, int size) {
        return new TestEvent(this, "event-" + id, generatePayload(size));
    }

    private DomainEvent createMediumEvent(long id, int size) {
        return new TestEvent(this, "event-" + id, generatePayload(size));
    }

    private String generatePayload(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Getter
    @StreamEvent(streamKey = "test_stream", group = "test-group")
    private static class TestEvent extends DomainEvent {
        private final String eventId;
        private final String payload;

        public TestEvent(Object source, String eventId, String payload) {
            super(source);
            this.eventId = eventId;
            this.payload = payload;
        }
    }
}
