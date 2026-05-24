package io.github.dekkerding.examples.infrastructure.delayed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于时间轮的延迟任务调度器
 *
 * <p>适用于短延迟任务（< 60秒）：
 * <ul>
 *   <li>时间精度: 100ms</li>
 *   <li>轮大小: 600槽位 (60秒)</li>
 *   <li>优势: O(1)任务插入，低CPU占用</li>
 *   <li>备份: 任务持久化到Redis，应用重启可恢复</li>
 * </ul>
 *
 * <p>时间轮结构：
 * <pre>
 *     0ms    100ms  200ms        59900ms
 *   ┌────┐ ┌────┐ ┌────┐ ...  ┌────┐
 *   │ T1 │ │    │ │ T2 │      │ T3 │
 *   └────┘ └────┘ └────┘      └────┘
 *      ↑
 *   currentTick (每100ms移动)
 * </pre>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class TimingWheelScheduler {

    /**
     * 时间刻度：100ms
     */
    private static final int TICK_DURATION = 100;

    /**
     * 时间轮大小：600槽位 = 60秒
     */
    private static final int WHEEL_SIZE = 600;

    /**
     * 最大延迟时长：60秒
     */
    private static final long MAX_DELAY_MILLIS = WHEEL_SIZE * TICK_DURATION;

    /**
     * Redis备份前缀
     */
    private static final String BACKUP_PREFIX = "delayed:wheel:backup:";

    /**
     * 任务桶数组
     */
    private final List<DelayedTask>[] buckets;

    /**
     * 当前刻度
     */
    private final AtomicInteger currentTick;

    /**
     * 定时调度器
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 任务分发器
     */
    private final DelayedTaskDispatcher taskDispatcher;

    /**
     * Redis模板（用于备份）
     */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 构造函数
     */
    @SuppressWarnings("unchecked")
    public TimingWheelScheduler(DelayedTaskDispatcher taskDispatcher) {
        this.taskDispatcher = taskDispatcher;
        this.buckets = new List[WHEEL_SIZE];
        for (int i = 0; i < WHEEL_SIZE; i++) {
            buckets[i] = new CopyOnWriteArrayList<>();
        }
        this.currentTick = new AtomicInteger(0);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "timing-wheel-scheduler")
        );
    }

    /**
     * 启动时间轮
     */
    @PostConstruct
    public void start() {
        log.info("启动时间轮调度器: tickDuration={}ms, wheelSize={}", TICK_DURATION, WHEEL_SIZE);

        // 启动定时任务，每TICK_DURATION毫秒移动一次指针
        scheduler.scheduleAtFixedRate(
            this::advanceClock,
            0,
            TICK_DURATION,
            TimeUnit.MILLISECONDS
        );

        // 从Redis恢复未执行的任务
        recoverTasks();

        log.info("时间轮调度器已启动");
    }

    /**
     * 停止时间轮
     */
    @PreDestroy
    public void stop() {
        log.info("停止时间轮调度器...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("时间轮调度器已停止");
    }

    /**
     * 调度延迟任务
     *
     * @param task 延迟任务
     * @return 是否调度成功
     */
    public boolean schedule(DelayedTask task) {
        long delay = task.getDelayMillis();

        // 检查延迟时长
        if (delay <= 0) {
            log.warn("延迟时间必须大于0: {}", delay);
            return false;
        }

        if (delay >= MAX_DELAY_MILLIS) {
            log.warn("延迟时间超过最大值{}ms，请使用SortedSet: {}", MAX_DELAY_MILLIS, delay);
            return false;
        }

        // 计算目标槽位
        int ticks = (int) ((delay + TICK_DURATION - 1) / TICK_DURATION); // 向上取整
        int bucketIndex = (currentTick.get() + ticks) % WHEEL_SIZE;

        // 添加到桶中
        buckets[bucketIndex].add(task);
        task.setStatus(DelayedTask.TaskStatus.SCHEDULED);
        task.setStrategy(DelayedTask.ScheduleStrategy.TIMING_WHEEL);

        log.debug("任务已调度到时间轮: taskId={}, bucketIndex={}, delay={}ms",
                task.getId(), bucketIndex, delay);

        // 备份到Redis
        backupTask(task);

        return true;
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    public boolean cancel(String taskId) {
        for (List<DelayedTask> bucket : buckets) {
            for (DelayedTask task : bucket) {
                if (task.getId().equals(taskId)) {
                    bucket.remove(task);
                    task.setStatus(DelayedTask.TaskStatus.CANCELLED);
                    removeBackup(taskId);
                    log.info("任务已取消: taskId={}", taskId);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 时间轮前进一格
     */
    private void advanceClock() {
        int tick = currentTick.getAndIncrement() % WHEEL_SIZE;
        List<DelayedTask> bucket = buckets[tick];

        if (bucket.isEmpty()) {
            return;
        }

        log.trace("时间轮前进: tick={}, bucketSize={}", tick, bucket.size());

        // 取出所有任务
        List<DelayedTask> tasks = new ArrayList<>(bucket);
        bucket.clear();

        // 过滤已取消的任务
        tasks.removeIf(t -> t.getStatus() == DelayedTask.TaskStatus.CANCELLED);

        // 分发到期任务
        for (DelayedTask task : tasks) {
            if (task.isExpired()) {
                task.setStatus(DelayedTask.TaskStatus.EXECUTING);
                taskDispatcher.dispatch(task);
                removeBackup(task.getId());
            } else {
                // 未到期，重新调度（可能由于时间精度问题）
                long remainingDelay = task.getDelayMillis();
                if (remainingDelay < MAX_DELAY_MILLIS) {
                    schedule(task);
                } else {
                    log.warn("任务延迟超出时间轮范围，降级到SortedSet: taskId={}", task.getId());
                    taskDispatcher.dispatchToSortedSet(task);
                }
            }
        }
    }

    /**
     * 备份任务到Redis
     */
    private void backupTask(DelayedTask task) {
        if (redisTemplate == null) {
            return;
        }

        try {
            String key = BACKUP_PREFIX + task.getId();
            String value = serializeTask(task);
            redisTemplate.opsForValue().set(key, value, MAX_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("备份任务到Redis失败: taskId={}", task.getId(), e);
        }
    }

    /**
     * 从Redis移除备份
     */
    private void removeBackup(String taskId) {
        if (redisTemplate == null) {
            return;
        }

        try {
            String key = BACKUP_PREFIX + taskId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("移除Redis备份失败: taskId={}", taskId, e);
        }
    }

    /**
     * 从Redis恢复任务
     */
    private void recoverTasks() {
        if (redisTemplate == null) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(BACKUP_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            log.info("从Redis恢复时间轮任务: count={}", keys.size());

            for (String key : keys) {
                try {
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        DelayedTask task = deserializeTask(value);
                        if (task != null && task.getStatus() == DelayedTask.TaskStatus.SCHEDULED) {
                            // 重新调度
                            long remainingDelay = task.getDelayMillis();
                            if (remainingDelay > 0 && remainingDelay < MAX_DELAY_MILLIS) {
                                schedule(task);
                                log.info("恢复任务: taskId={}, remainingDelay={}ms", task.getId(), remainingDelay);
                            } else {
                                // 已到期或超时，直接分发
                                if (task.isExpired()) {
                                    taskDispatcher.dispatch(task);
                                }
                                redisTemplate.delete(key);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("恢复任务失败: key={}", key, e);
                }
            }
        } catch (Exception e) {
            log.error("恢复时间轮任务失败", e);
        }
    }

    /**
     * 序列化任务
     */
    private String serializeTask(DelayedTask task) {
        // 简化实现，实际应使用JSON
        return String.format("%s|%d|%d|%s",
                task.getId(),
                task.getExecuteTimeMillis(),
                task.getCreateTimeMillis(),
                task.getTargetStream());
    }

    /**
     * 反序列化任务
     */
    private DelayedTask deserializeTask(String value) {
        // 简化实现，实际应使用JSON
        try {
            String[] parts = value.split("\\|");
            if (parts.length >= 4) {
                return DelayedTask.builder()
                        .id(parts[0])
                        .executeTimeMillis(Long.parseLong(parts[1]))
                        .createTimeMillis(Long.parseLong(parts[2]))
                        .targetStream(parts[3])
                        .status(DelayedTask.TaskStatus.SCHEDULED)
                        .strategy(DelayedTask.ScheduleStrategy.TIMING_WHEEL)
                        .build();
            }
        } catch (Exception e) {
            log.warn("反序列化任务失败: {}", value, e);
        }
        return null;
    }

    /**
     * 获取待处理任务数
     */
    public int getPendingTaskCount() {
        int count = 0;
        for (List<DelayedTask> bucket : buckets) {
            count += bucket.size();
        }
        return count;
    }

    /**
     * 获取当前槽位待处理任务数
     */
    public int getCurrentBucketSize() {
        return buckets[currentTick.get() % WHEEL_SIZE].size();
    }
}
