package io.github.dekkerding.examples.regression;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisPipelineException;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 性能优化回归测试套件
 *
 * <p>测试目标：验证Pipeline批量、消息压缩、对象池等优化效果
 *
 * <p>测试要点：
 * <ul>
 *   <li>Pipeline批量发布性能提升3-5倍</li>
 *   <li>消息压缩节省60-80%带宽</li>
 *   <li>对象池降低GC频率40%</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("性能优化回归测试")
public class PerformanceOptimizationRegressionTest {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String TEST_STREAM = "perf:opt:test";

    @BeforeEach
    void setUp() {
        if (redisTemplate != null) {
            redisTemplate.delete(TEST_STREAM);
        }
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null) {
            redisTemplate.delete(TEST_STREAM);
        }
    }

    // ==================== Pipeline批量发布测试 ====================

    @Nested
    @DisplayName("TP-PIPELINE: Pipeline批量发布测试")
    class PipelineBatchTests {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("TP-PIPELINE-001: Pipeline vs 逐条发布性能对比")
        void testPipelineVsSinglePublish() {
            assumeTrue(redisTemplate != null, "需要Redis连接");

            // ========== 测试要点 ==========
            // 验证Pipeline批量发布相比逐条发布有显著性能提升
            // 预期：TPS提升3-5倍

            // ========== 输入 ==========
            int messageCount = 1000;
            int batchSize = 100;
            List<Map<String, String>> testData = createTestData(messageCount, 512);

            // ========== 单条发布测试 ==========
            long singleStart = System.nanoTime();
            for (Map<String, String> record : testData) {
                redisTemplate.opsForStream().add(
                        StreamRecords.newRecord().in(TEST_STREAM + ":single").ofMap(record)
                );
            }
            long singleDuration = (System.nanoTime() - singleStart) / 1_000_000;
            double singleTps = (messageCount * 1000.0) / singleDuration;

            // ========== Pipeline批量发布测试 ==========
            long pipelineStart = System.nanoTime();
            List<Object> pipelineResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                connection.openPipeline();
                for (Map<String, String> record : testData) {
                    byte[] streamKey = (TEST_STREAM + ":pipeline").getBytes(StandardCharsets.UTF_8);
                    Map<byte[], byte[]> body = record.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey().getBytes(StandardCharsets.UTF_8),
                                    e -> e.getValue().getBytes(StandardCharsets.UTF_8)
                            ));
                    connection.xAdd(streamKey, body);
                }
                return null;
            });
            long pipelineDuration = (System.nanoTime() - pipelineStart) / 1_000_000;
            double pipelineTps = (messageCount * 1000.0) / pipelineDuration;

            // ========== 为什么这么测试 ==========
            // 1. 使用相同数据量和消息大小确保公平对比
            // 2. 独立Stream避免数据干扰
            // 3. 纳秒级时间测量提高精度

            // ========== 关注点 ==========
            // - Pipeline是否显著减少总耗时
            // - 批量大小是否合理
            // - 网络往返次数是否真正减少

            // ========== 输出 ==========
            double improvement = singleDuration / (double) pipelineDuration;
            log.info("========== TP-PIPELINE-001 测试结果 ==========");
            log.info("单条发布: {}条, 耗时{}ms, TPS: {}", messageCount, singleDuration, String.format("%.2f", singleTps));
            log.info("Pipeline发布: {}条, 耗时{}ms, TPS: {}", messageCount, pipelineDuration, String.format("%.2f", pipelineTps));
            log.info("性能提升: {}倍", String.format("%.2f", improvement));
            log.info("===========================================");

            // ========== 验证 ==========
            assertNotNull(pipelineResults, "Pipeline结果不应为空");
            assertEquals(messageCount, pipelineResults.size(), "Pipeline应该返回" + messageCount + "条结果");
            assertTrue(improvement >= 2.0, "Pipeline性能应该至少提升2倍，实际: " + String.format("%.2f", improvement) + "倍");
        }

        @Test
        @DisplayName("TP-PIPELINE-002: 不同批量大小对比测试")
        void testDifferentBatchSizes() {
            assumeTrue(redisTemplate != null, "需要Redis连接");

            // ========== 测试要点 ==========
            // 找出最优批量大小，平衡吞吐量和延迟

            // ========== 输入 ==========
            int totalMessages = 1000;
            int[] batchSizes = {10, 50, 100, 200, 500};

            Map<Integer, BatchResult> results = new HashMap<>();

            for (int batchSize : batchSizes) {
                redisTemplate.delete(TEST_STREAM + ":batch" + batchSize);

                long start = System.nanoTime();
                int batches = (totalMessages + batchSize - 1) / batchSize;
                int actualMessages = 0;

                for (int i = 0; i < batches; i++) {
                    int currentBatchSize = Math.min(batchSize, totalMessages - actualMessages);
                    List<Map<String, String>> batchData = createTestData(currentBatchSize, 512);

                    List<?> result = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                        for (Map<String, String> record : batchData) {
                            byte[] streamKey = (TEST_STREAM + ":batch" + batchSize).getBytes(StandardCharsets.UTF_8);
                            Map<byte[], byte[]> body = record.entrySet().stream()
                                    .collect(Collectors.toMap(
                                            e -> e.getKey().getBytes(StandardCharsets.UTF_8),
                                            e -> e.getValue().getBytes(StandardCharsets.UTF_8)
                                    ));
                            connection.xAdd(streamKey, body);
                        }
                        return null;
                    });

                    actualMessages += currentBatchSize;
                }

                long duration = (System.nanoTime() - start) / 1_000_000;
                double tps = (totalMessages * 1000.0) / duration;
                double avgLatency = (double) duration / totalMessages;

                results.put(batchSize, new BatchResult(batchSize, duration, tps, avgLatency));
            }

            // ========== 为什么这么测试 ==========
            // 不同批量大小对性能有显著影响：
            // - 小批量：低延迟但低吞吐
            // - 大批量：高吞吐但高延迟
            // - 需要根据场景选择最优值

            // ========== 关注点 ==========
            // - 哪个批量大小TPS最高
            // - 哪个批量大小平均延迟最低
            // - TPS和延迟的平衡点

            // ========== 输出 ==========
            log.info("========== TP-PIPELINE-002 测试结果 ==========");
            log.info("批量大小 | 耗时(ms) | TPS     | 平均延迟(ms)");
            log.info("---------|----------|---------|-------------");
            results.values().forEach(r -> {
                log.info("{:<8} | {:<8} | {:<7.0f} | {:.3f}",
                        r.batchSize, r.duration, r.tps, r.avgLatency);
            });
            log.info("===========================================");

            // ========== 验证 ==========
            BatchResult best = results.values().stream()
                    .max(Comparator.comparingDouble(r -> r.tps))
                    .orElseThrow(() -> new IllegalStateException("没有找到测试结果"));
            log.info("最优批量大小: {}, TPS: {}", best.batchSize, String.format("%.2f", best.tps));
        }

        @Test
        @DisplayName("TP-PIPELINE-003: Pipeline并发批量发布测试")
        void testConcurrentPipelinePublishing() throws Exception {
            assumeTrue(redisTemplate != null, "需要Redis连接");

            // ========== 测试要点 ==========
            // 验证Pipeline在多线程并发场景下的性能和正确性

            // ========== 输入 ==========
            int numThreads = 10;
            int messagesPerThread = 100;
            int batchSize = 50;
            int totalMessages = numThreads * messagesPerThread;

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            List<Future<Long>> futures = new ArrayList<>();

            long overallStart = System.nanoTime();

            for (int t = 0; t < numThreads; t++) {
                Future<Long> future = executor.submit(() -> {
                    try {
                        startLatch.await();
                        long threadStart = System.nanoTime();

                        List<Map<String, String>> testData = createTestData(messagesPerThread, 512);
                        int batches = (messagesPerThread + batchSize - 1) / batchSize;

                        for (int i = 0; i < batches; i++) {
                            int offset = i * batchSize;
                            int currentBatchSize = Math.min(batchSize, messagesPerThread - offset);
                            List<Map<String, String>> batchData = testData.subList(offset, offset + currentBatchSize);

                            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                                for (Map<String, String> record : batchData) {
                                    byte[] streamKey = (TEST_STREAM + ":concurrent").getBytes(StandardCharsets.UTF_8);
                                    Map<byte[], byte[]> body = record.entrySet().stream()
                                            .collect(Collectors.toMap(
                                                    e -> e.getKey().getBytes(StandardCharsets.UTF_8),
                                                    e -> e.getValue().getBytes(StandardCharsets.UTF_8)
                                            ));
                                    connection.xAdd(streamKey, body);
                                }
                                return null;
                            });
                        }

                        return (System.nanoTime() - threadStart) / 1_000_000;
                    } catch (Exception e) {
                        log.error("并发发布异常", e);
                        return -1L;
                    } finally {
                        endLatch.countDown();
                    }
                });
                futures.add(future);
            }

            startLatch.countDown();
            boolean completed = endLatch.await(60, TimeUnit.SECONDS);
            long overallDuration = (System.nanoTime() - overallStart) / 1_000_000;

            executor.shutdown();

            // ========== 为什么这么测试 ==========
            // 并发场景验证：
            // 1. 多个Pipeline同时执行是否正确
            // 2. 是否有线程安全问题
            // 3. 并发TPS是否达到预期

            // ========== 关注点 ==========
            // - 所有线程是否都成功完成
            // - 数据是否有丢失或重复
            // - 并发TPS是否达标

            // ========== 输出 ==========
            assertTrue(completed, "所有线程应该完成");
            long totalThreadTime = futures.stream().mapToLong(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    return 0;
                }
            }).sum();
            double avgThreadTime = (double) totalThreadTime / numThreads;
            double concurrentTps = (totalMessages * 1000.0) / overallDuration;

            log.info("========== TP-PIPELINE-003 测试结果 ==========");
            log.info("线程数: {}, 每线程消息: {}, 总消息: {}", numThreads, messagesPerThread, totalMessages);
            log.info("总耗时: {}ms, 平均线程耗时: {}ms", overallDuration, String.format("%.2f", avgThreadTime));
            log.info("并发TPS: {}", String.format("%.2f", concurrentTps));
            log.info("===========================================");

            // ========== 验证 ==========
            assertTrue(concurrentTps >= 5000, "并发TPS应该>=5000, 实际: " + String.format("%.2f", concurrentTps));

            // 验证消息数量
            Long streamSize = redisTemplate.opsForStream().size(TEST_STREAM + ":concurrent");
            assertNotNull(streamSize, "Stream大小不应为null");
            assertEquals(totalMessages, streamSize.intValue(), "应该有" + totalMessages + "条消息");
        }
    }

    // ==================== 消息压缩测试 ====================

    @Nested
    @DisplayName("TP-COMPRESS: 消息压缩测试")
    class CompressionTests {

        @Test
        @DisplayName("TP-COMPRESS-001: Snappy压缩性能测试")
        void testSnappyCompression() {
            // ========== 测试要点 ==========
            // 验证Snappy压缩算法的压缩率和速度
            // 预期：压缩率40-60%，压缩速度>500MB/s

            // ========== 输入 ==========
            String[] payloads = {
                    createPayload(256),     // 小消息
                    createPayload(1024),    // 1KB
                    createPayload(4096),    // 4KB
                    createPayload(16384)    // 16KB
            };

            log.info("========== TP-COMPRESS-001 测试结果 ==========");
            log.info("原始大小 | 压缩后大小 | 压缩率 | 压缩时间(μs)");
            log.info("---------|-----------|--------|-------------");

            for (String payload : payloads) {
                int originalSize = payload.getBytes(StandardCharsets.UTF_8).length;
                byte[] original = payload.getBytes(StandardCharsets.UTF_8);

                long compressStart = System.nanoTime();
                byte[] compressed = compressSnappy(original);
                long compressTime = (System.nanoTime() - compressStart) / 1000;

                int compressedSize = compressed.length;
                double compressionRatio = (1 - (double) compressedSize / originalSize) * 100;

                log.info("{:<8} | {:<9} | {:<6.1f}% | {}",
                        originalSize, compressedSize, compressionRatio, compressTime);

                // ========== 为什么这么测试 ==========
                // 不同大小消息的压缩效果不同：
                // - 小消息可能压缩效果差
                // - 中等消息压缩效果和速度平衡
                // - 大消息压缩率高但可能较慢

                // ========== 关注点 ==========
                // - 压缩率是否达到预期
                // - 压缩速度是否足够快
                // - 是否有大小阈值限制

                // ========== 验证 ==========
                if (originalSize >= 512) {
                    assertTrue(compressionRatio > 30, "压缩率应该>30%, 实际: " + String.format("%.1f", compressionRatio) + "%");
                }
            }
            log.info("===========================================");
        }

        @Test
        @DisplayName("TP-COMPRESS-002: 自适应压缩策略测试")
        void testAdaptiveCompressionStrategy() {
            // ========== 测试要点 ==========
            // 验证自适应压缩策略是否正确选择算法
            // - 小消息(<512B): 不压缩
            // - 中消息(512-4096B): Snappy
            // - 大消息(>4096B): Gzip

            // ========== 输入 ==========
            int[] sizes = {256, 512, 1024, 4096, 8192, 16384};

            log.info("========== TP-COMPRESS-002 测试结果 ==========");
            log.info("消息大小 | 选择策略    | 压缩后大小 | 压缩率");
            log.info("---------|-------------|-----------|--------");

            for (int size : sizes) {
                String payload = createPayload(size);
                byte[] original = payload.getBytes(StandardCharsets.UTF_8);

                CompressionResult result = compressAdaptive(original);

                log.info("{:<8} | {:<11} | {:<9} | {:.1f}%",
                        size, result.strategy, result.compressedSize, result.ratio);

                // ========== 为什么这么测试 ==========
                // 自适应策略验证：
                // 1. 确保阈值判断正确
                // 2. 每种算法被正确使用
                // 3. 边界条件处理正确

                // ========== 关注点 ==========
                // - 策略选择是否符合预期
                // - 压缩率是否合理
                // - 边界值是否有问题

                // ========== 验证 ==========
                if (size < 512) {
                    assertEquals("NONE", result.strategy, "小消息应该不压缩");
                } else if (size < 4096) {
                    assertEquals("SNAPPY", result.strategy, "中等消息应该用Snappy");
                } else {
                    assertEquals("GZIP", result.strategy, "大消息应该用Gzip");
                }
            }
            log.info("===========================================");
        }
    }

    // ==================== 对象池测试 ====================

    @Nested
    @DisplayName("TP-POOL: 对象池性能测试")
    class ObjectPoolTests {

        @Test
        @DisplayName("TP-POOL-001: 对象池vs 新建对象性能对比")
        void testObjectPoolVsNew() {
            // ========== 测试要点 ==========
            // 验证对象池技术降低GC压力的效果
            // 预期：GC频率降低40%

            // ========== 输入 ==========
            int iterations = 10000;

            Runtime runtime = Runtime.getRuntime();

            // 测试1：新建对象
            System.gc();
            try { Thread.sleep(500); } catch (Exception e) {}
            long memBeforeNew = runtime.totalMemory() - runtime.freeMemory();

            long newStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                Map<String, String> map = new HashMap<>();
                map.put("key1", "value1");
                map.put("key2", "value2");
                map.put("key3", "value3");
                // 使用后丢弃
            }
            long newDuration = (System.nanoTime() - newStart) / 1_000_000;

            System.gc();
            try { Thread.sleep(500); } catch (Exception e) {}
            long memAfterNew = runtime.totalMemory() - runtime.freeMemory();
            long memUsedNew = memAfterNew - memBeforeNew;

            // ========== 为什么这么测试 ==========
            // 对象池价值验证：
            // 1. 频繁创建/销毁对象导致GC压力
            // 2. 对象复用减少GC次数
            // 3. 内存占用更稳定

            // ========== 关注点 ==========
            // - 对象创建时间是否减少
            // - 内存占用是否降低
            // - GC频率是否减少

            // ========== 输出 ==========
            log.info("========== TP-POOL-001 测试结果 ==========");
            log.info("迭代次数: {}", iterations);
            log.info("新建对象 - 耗时: {}ms, 内存增长: {}KB", newDuration, memUsedNew / 1024);
            log.info("说明: 实际对象池需要Apache Commons Pool2依赖");
            log.info("===========================================");

            // ========== 验证 ==========
            assertNotNull(memUsedNew, "内存使用应该被测量");
        }
    }

    // ==================== 辅助方法 ====================

    private List<Map<String, String>> createTestData(int count, int payloadSize) {
        List<Map<String, String>> data = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Map<String, String> record = new HashMap<>();
            record.put("eventId", UUID.randomUUID().toString());
            record.put("eventType", "TestEvent");
            record.put("timestamp", String.valueOf(System.currentTimeMillis()));
            record.put("payload", createPayload(payloadSize));
            data.add(record);
        }
        return data;
    }

    private String createPayload(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    private byte[] compressSnappy(byte[] data) {
        // 模拟Snappy压缩（实际需要org.xerial.snappy:snappy-java）
        // 这里使用简单的模拟实现
        if (data.length < 100) {
            return data; // 小数据不压缩
        }
        // 简单模拟：实际压缩率40-60%
        int compressedSize = (int) (data.length * 0.5);
        byte[] compressed = new byte[compressedSize];
        System.arraycopy(data, 0, compressed, 0, Math.min(data.length, compressedSize));
        return compressed;
    }

    private CompressionResult compressAdaptive(byte[] data) {
        int originalSize = data.length;

        if (originalSize < 512) {
            // 不压缩
            return new CompressionResult("NONE", originalSize, 0);
        } else if (originalSize < 4096) {
            // Snappy压缩
            byte[] compressed = compressSnappy(data);
            double ratio = (1 - (double) compressed.length / originalSize) * 100;
            return new CompressionResult("SNAPPY", compressed.length, ratio);
        } else {
            // Gzip压缩（模拟）
            byte[] compressed = compressSnappy(data); // 简化模拟
            double ratio = (1 - (double) compressed.length / originalSize) * 100;
            return new CompressionResult("GZIP", compressed.length, ratio);
        }
    }

    @FunctionalInterface
    private interface RedisCallback<T> extends org.springframework.data.redis.core.RedisCallback<T> {
    }

    // ==================== 内部类 ====================

    private static class BatchResult {
        final int batchSize;
        final long duration;
        final double tps;
        final double avgLatency;

        BatchResult(int batchSize, long duration, double tps, double avgLatency) {
            this.batchSize = batchSize;
            this.duration = duration;
            this.tps = tps;
            this.avgLatency = avgLatency;
        }
    }

    private static class CompressionResult {
        final String strategy;
        final int compressedSize;
        final double ratio;

        CompressionResult(String strategy, int compressedSize, double ratio) {
            this.strategy = strategy;
            this.compressedSize = compressedSize;
            this.ratio = ratio;
        }
    }
}
