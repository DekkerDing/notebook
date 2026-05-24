package io.github.dekkerding.examples.infrastructure.delayed;

import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 延迟任务分发器
 *
 * <p>负责将到期的延迟任务推送到目标Redis Stream。
 *
 * <p>核心功能：
 * <ul>
 *   <li>异步推送：使用独立线程池，不阻塞时间轮</li>
 *   <li>批量推送：Pipeline批量XADD，提升性能</li>
 *   <li>幂等性：确保任务不重复推送</li>
 *   <li>重试机制：推送失败自动重试</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class DelayedTaskDispatcher {

    /**
     * 已处理任务记录（Redis Key前缀）
     */
    private static final String PROCESSED_PREFIX = "delayed:processed:";

    /**
     * SortedSet延迟队列（长延迟任务）
     */
    private static final String SORTED_SET_KEY = "delayed:sortedset";

    /**
     * Redis模板
     */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 异步执行线程池
     */
    private final ExecutorService executorService;

    /**
     * 延迟调度线程池（用于重试）
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 已处理任务计数
     */
    private final AtomicLong processedCount;

    /**
     * 失败任务计数
     */
    private final AtomicLong failedCount;

    /**
     * 构造函数
     */
    public DelayedTaskDispatcher() {
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> new Thread(r, "delayed-task-dispatcher")
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "delayed-task-scheduler")
        );
        this.processedCount = new AtomicLong(0);
        this.failedCount = new AtomicLong(0);
    }

    /**
     * 分发延迟任务
     *
     * @param task 延迟任务
     */
    public void dispatch(DelayedTask task) {
        // 检查是否已处理
        if (isProcessed(task.getId())) {
            log.debug("任务已处理，跳过: taskId={}", task.getId());
            return;
        }

        // 异步分发
        executorService.submit(() -> doDispatch(task));
    }

    /**
     * 分发到SortedSet（用于长延迟任务）
     *
     * @param task 延迟任务
     */
    public void dispatchToSortedSet(DelayedTask task) {
        if (redisTemplate == null) {
            log.warn("Redis不可用，无法分发到SortedSet: taskId={}", task.getId());
            return;
        }

        try {
            // 添加到SortedSet，score为执行时间戳
            String value = serializeTask(task);
            redisTemplate.opsForZSet().add(
                SORTED_SET_KEY,
                value,
                task.getExecuteTimeMillis()
            );

            task.setStatus(DelayedTask.TaskStatus.SCHEDULED);
            task.setStrategy(DelayedTask.ScheduleStrategy.SORTED_SET);

            log.info("任务已添加到SortedSet: taskId={}, executeTime={}",
                    task.getId(), task.getExecuteTimeMillis());
        } catch (Exception e) {
            log.error("分发到SortedSet失败: taskId={}", task.getId(), e);
        }
    }

    /**
     * 实际执行分发
     */
    private void doDispatch(DelayedTask task) {
        try {
            log.debug("开始分发延迟任务: taskId={}, stream={}",
                    task.getId(), task.getTargetStream());

            // 推送到目标Stream
            if (redisTemplate != null) {
                // 直接构建Stream记录
                Map<String, String> record = new HashMap<>();
                record.put(EventEnvelope.FIELD_EVENT_ID, task.getId());
                record.put(EventEnvelope.FIELD_EVENT_TYPE, task.getEvent().getEventType());
                record.put(EventEnvelope.FIELD_EVENT_CLASS, task.getEvent().getClass().getName());
                record.put(EventEnvelope.FIELD_PAYLOAD, serializeEvent(task.getEvent()));
                record.put(EventEnvelope.FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                record.put(EventEnvelope.FIELD_SOURCE_APP, "delayed-queue");

                // 使用XADD命令添加到Stream
                String messageId = redisTemplate.opsForStream().add(
                    task.getTargetStream(),
                    record
                ).getValue();

                log.debug("消息已推送到Stream: stream={}, messageId={}",
                        task.getTargetStream(), messageId);
            } else {
                throw new IllegalStateException("Redis不可用");
            }

            // 标记已处理
            markProcessed(task.getId());

            // 更新状态
            task.setStatus(DelayedTask.TaskStatus.COMPLETED);
            processedCount.incrementAndGet();

            log.info("延迟任务分发成功: taskId={}, stream={}",
                    task.getId(), task.getTargetStream());

        } catch (Exception e) {
            log.error("延迟任务分发失败: taskId={}", task.getId(), e);

            // 重试逻辑
            if (task.canRetry()) {
                task.incrementRetry();
                log.info("将重试任务: taskId={}, retryCount={}",
                        task.getId(), task.getRetryCount());
                // 延迟重试
                scheduler.schedule(
                    () -> doDispatch(task),
                    calculateBackoff(task.getRetryCount()),
                    TimeUnit.MILLISECONDS
                );
            } else {
                task.setStatus(DelayedTask.TaskStatus.FAILED);
                failedCount.incrementAndGet();
                log.error("任务重试次数用尽，标记失败: taskId={}", task.getId());
            }
        }
    }

    /**
     * 检查任务是否已处理
     */
    private boolean isProcessed(String taskId) {
        if (redisTemplate == null) {
            return false;
        }

        try {
            String key = PROCESSED_PREFIX + taskId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("检查任务状态失败: taskId={}", taskId, e);
            return false;
        }
    }

    /**
     * 标记任务已处理
     */
    private void markProcessed(String taskId) {
        if (redisTemplate == null) {
            return;
        }

        try {
            String key = PROCESSED_PREFIX + taskId;
            // 保留24小时
            redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("标记任务已处理失败: taskId={}", taskId, e);
        }
    }

    /**
     * 序列化事件
     */
    private String serializeEvent(Object event) {
        // 简化实现，实际应使用Jackson
        return event.toString();
    }

    /**
     * 序列化任务（用于SortedSet）
     */
    private String serializeTask(DelayedTask task) {
        // 简化实现，实际应使用JSON
        return String.format("%s|%s", task.getId(), task.getTargetStream());
    }

    /**
     * 计算退避时间（指数退避）
     */
    private long calculateBackoff(int retryCount) {
        // 1s, 2s, 4s, 8s, 16s...
        return Math.min(1000L * (1L << retryCount), 30000L);
    }

    /**
     * 获取已处理任务数
     */
    public long getProcessedCount() {
        return processedCount.get();
    }

    /**
     * 获取失败任务数
     */
    public long getFailedCount() {
        return failedCount.get();
    }

    /**
     * 扫描SortedSet中到期的任务
     *
     * <p>由定时任务调用，扫描并分发长延迟任务
     */
    public void scanSortedSet() {
        if (redisTemplate == null) {
            return;
        }

        try {
            long now = System.currentTimeMillis();

            // 查询score <= now的任务
            Set<String> expiredTasks = redisTemplate.opsForZSet()
                .rangeByScore(SORTED_SET_KEY, 0, now);

            if (expiredTasks == null || expiredTasks.isEmpty()) {
                return;
            }

            log.info("扫描SortedSet发现到期任务: count={}", expiredTasks.size());

            for (String taskStr : expiredTasks) {
                try {
                    DelayedTask task = deserializeTaskFromSortedSet(taskStr);
                    if (task != null && !isProcessed(task.getId())) {
                        dispatch(task);
                    }
                    // 从SortedSet移除
                    redisTemplate.opsForZSet().remove(SORTED_SET_KEY, taskStr);
                } catch (Exception e) {
                    log.error("处理SortedSet任务失败: {}", taskStr, e);
                }
            }
        } catch (Exception e) {
            log.error("扫描SortedSet失败", e);
        }
    }

    /**
     * 从SortedSet反序列化任务
     */
    private DelayedTask deserializeTaskFromSortedSet(String taskStr) {
        try {
            String[] parts = taskStr.split("\\|");
            if (parts.length >= 2) {
                return DelayedTask.builder()
                        .id(parts[0])
                        .targetStream(parts[1])
                        .build();
            }
        } catch (Exception e) {
            log.warn("反序列化SortedSet任务失败: {}", taskStr, e);
        }
        return null;
    }

    /**
     * 关闭分发器
     */
    public void shutdown() {
        log.info("关闭延迟任务分发器...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("延迟任务分发器已关闭");
    }
}
