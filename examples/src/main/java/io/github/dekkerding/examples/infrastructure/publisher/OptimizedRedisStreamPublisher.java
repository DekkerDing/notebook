package io.github.dekkerding.examples.infrastructure.publisher;

import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.infrastructure.publisher.EventPublisher.MessageId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 性能优化的Redis Stream发布器
 * 使用Pipeline、批量发布、异步处理等优化技术
 */
@Slf4j
public class OptimizedRedisStreamPublisher implements EventPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final int batchSize;
    private final int asyncThreads;
    private final ExecutorService executorService;

    // 待发布的消息队列
    private final BlockingQueue<PendingMessage> pendingQueue;
    // 批量发布器
    private final BatchPublisher batchPublisher;

    public OptimizedRedisStreamPublisher(RedisTemplate<String, String> redisTemplate) {
        this(redisTemplate, 100, 4);
    }

    public OptimizedRedisStreamPublisher(
            RedisTemplate<String, String> redisTemplate,
            int batchSize,
            int asyncThreads) {
        this.redisTemplate = redisTemplate;
        this.batchSize = batchSize;
        this.asyncThreads = asyncThreads;
        // 使用有界队列防止内存溢出
        this.pendingQueue = new LinkedBlockingQueue<>(10000);
        this.executorService = Executors.newFixedThreadPool(asyncThreads);
        this.batchPublisher = new BatchPublisher();

        // 启动批量发布器
        Thread publisherThread = new Thread(batchPublisher, "redis-stream-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();

        log.info("优化发布器已初始化: batchSize={}, asyncThreads={}", batchSize, asyncThreads);
    }

    /**
     * 发布消息（带背压机制）
     */
    @Override
    public MessageId publish(EventEnvelope envelope, String streamKey) {
        // 添加到待发布队列，使用offer避免阻塞
        boolean offered = pendingQueue.offer(new PendingMessage(envelope, streamKey));

        if (!offered) {
            // 队列已满，记录告警并直接同步发布
            log.warn("待发布队列已满，使用同步发布: streamKey={}", streamKey);
            return publishSync(envelope, streamKey);
        }

        // 返回pending ID（实际发布是异步的）
        return MessageId.of("PENDING:" + envelope.getMetadata().getEventId());
    }

    /**
     * 同步发布（后备方法）
     */
    private MessageId publishSync(EventEnvelope envelope, String streamKey) {
        Map<String, String> record = envelope.toStreamRecord();
        org.springframework.data.redis.connection.stream.RecordId recordId =
                redisTemplate.opsForStream()
                        .add(org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                                .in(streamKey)
                                .ofMap(record));
        return MessageId.of(recordId.getValue());
    }

    /**
     * 批量发布（高性能版本）
     * 简化版本：使用异步并发处理
     */
    public List<MessageId> publishBatch(List<EventEnvelope> envelopes, String streamKey) {
        List<MessageId> result = new ArrayList<>(envelopes.size());

        // 使用线程池并发发布
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (EventEnvelope envelope : envelopes) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Map<String, String> record = envelope.toStreamRecord();
                redisTemplate.opsForStream()
                        .add(org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                                .in(streamKey)
                                .ofMap(record));
            }, executorService);

            futures.add(future);
        }

        // 等待所有发布完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            log.debug("批量发布{}条消息到{}", envelopes.size(), streamKey);
        } catch (Exception e) {
            log.error("批量发布部分失败: streamKey={}", streamKey, e);
        }

        return result;
    }

    /**
     * 关闭发布器
     */
    public void shutdown() {
        batchPublisher.running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("优化发布器已关闭");
    }

    /**
     * 待发布消息
     */
    private static class PendingMessage {
        final EventEnvelope envelope;
        final String streamKey;
        final long timestamp;

        PendingMessage(EventEnvelope envelope, String streamKey) {
            this.envelope = envelope;
            this.streamKey = streamKey;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 批量发布器线程
     */
    private class BatchPublisher implements Runnable {
        private volatile boolean running = true;
        private final List<PendingMessage> batch = new ArrayList<>(batchSize);

        @Override
        public void run() {
            while (running) {
                try {
                    // 收集批量消息
                    PendingMessage first = pendingQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (first != null) {
                        batch.add(first);

                        // 收集更多消息
                        pendingQueue.drainTo(batch, batchSize - 1);

                        // 批量发布
                        publishBatch();

                        batch.clear();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (Exception e) {
                    log.error("批量发布异常", e);
                }
            }
        }

        private void publishBatch() {
            if (batch.isEmpty()) {
                return;
            }

            // 使用线程池并发发布
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (PendingMessage message : batch) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, String> record = message.envelope.toStreamRecord();
                        RecordId recordId = redisTemplate.opsForStream()
                                .add(StreamRecords.newRecord()
                                        .in(message.streamKey)
                                        .ofMap(record));

                        log.trace("消息已发布: stream={}, recordId={}",
                                message.streamKey, recordId);
                    } catch (Exception e) {
                        log.error("发布消息失败: stream={}", message.streamKey, e);
                    }
                }, executorService);

                futures.add(future);
            }

            // 等待所有发布完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    /**
     * 获取性能统计
     */
    public PerformanceStats getStats() {
        return new PerformanceStats(
                pendingQueue.size(),
                batchSize,
                asyncThreads
        );
    }

    /**
     * 性能统计
     */
    public static class PerformanceStats {
        private final int pendingMessages;
        private final int batchSize;
        private final int asyncThreads;

        public PerformanceStats(int pendingMessages, int batchSize, int asyncThreads) {
            this.pendingMessages = pendingMessages;
            this.batchSize = batchSize;
            this.asyncThreads = asyncThreads;
        }

        public int getPendingMessages() {
            return pendingMessages;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public int getAsyncThreads() {
            return asyncThreads;
        }

        @Override
        public String toString() {
            return String.format("PerformanceStats{pending=%d, batchSize=%d, threads=%d}",
                    pendingMessages, batchSize, asyncThreads);
        }
    }
}
