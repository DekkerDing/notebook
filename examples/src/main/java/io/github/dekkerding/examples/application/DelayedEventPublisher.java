package io.github.dekkerding.examples.application;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.delayed.DelayedTask;
import io.github.dekkerding.examples.infrastructure.delayed.DelayedTaskDispatcher;
import io.github.dekkerding.examples.infrastructure.delayed.TimingWheelScheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 延迟事件发布器
 *
 * <p>提供无感接入的延迟消息发布API，支持：
 * <ul>
 *   <li>延迟N毫秒后发布</li>
 *   <li>指定时间点发布</li>
 *   <li>取消延迟任务</li>
 *   <li>查询任务状态</li>
 * </ul>
 *
 * <p>接入示例：
 * <pre>
 * &#64;Autowired
 * private DelayedEventPublisher delayedPublisher;
 *
 * // 延迟5秒发布
 * String taskId = delayedPublisher.publishDelayed(event, 5000);
 *
 * // 指定时间发布
 * taskId = delayedPublisher.publishDelayedAt(event, LocalDateTime.of(2024, 5, 25, 10, 0));
 *
 * // 取消任务
 * delayedPublisher.cancelDelayed(taskId);
 * </pre>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
public class DelayedEventPublisher {

    /**
     * 短延迟阈值：60秒
     */
    private static final long SHORT_DELAY_THRESHOLD = 60_000;

    /**
     * 时间轮调度器（短延迟）
     */
    private final TimingWheelScheduler timingWheelScheduler;

    /**
     * 任务分发器（长延迟）
     */
    private final DelayedTaskDispatcher taskDispatcher;

    /**
     * SortedSet扫描器
     */
    private final ScheduledExecutorService scanner;

    /**
     * 构造函数
     */
    public DelayedEventPublisher(
            TimingWheelScheduler timingWheelScheduler,
            DelayedTaskDispatcher taskDispatcher) {
        this.timingWheelScheduler = timingWheelScheduler;
        this.taskDispatcher = taskDispatcher;

        // 启动SortedSet扫描器（每秒扫描一次）
        this.scanner = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "delayed-queue-scanner")
        );
        this.scanner.scheduleAtFixedRate(
            taskDispatcher::scanSortedSet,
            1, 1, TimeUnit.SECONDS
        );
    }

    /**
     * 延迟N毫秒后发布事件
     *
     * @param event 领域事件
     * @param delayMillis 延迟毫秒数
     * @return 延迟任务ID（可用于取消）
     */
    public String publishDelayed(DomainEvent event, long delayMillis) {
        return publishDelayed(event, delayMillis, getDefaultStreamKey(event));
    }

    /**
     * 延迟N毫秒后发布事件到指定Stream
     *
     * @param event 领域事件
     * @param delayMillis 延迟毫秒数
     * @param streamKey 目标Stream键
     * @return 延迟任务ID
     */
    public String publishDelayed(DomainEvent event, long delayMillis, String streamKey) {
        if (event == null) {
            throw new IllegalArgumentException("事件不能为空");
        }
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("延迟时间必须大于0: " + delayMillis);
        }

        DelayedTask task = DelayedTask.builder()
                .id(generateTaskId())
                .event(event)
                .targetStream(streamKey)
                .createTimeMillis(System.currentTimeMillis())
                .executeTimeMillis(System.currentTimeMillis() + delayMillis)
                .status(DelayedTask.TaskStatus.PENDING)
                .build();

        return scheduleTask(task);
    }

    /**
     * 在指定时间点发布事件
     *
     * @param event 领域事件
     * @param executeTime 执行时间
     * @return 延迟任务ID
     */
    public String publishDelayedAt(DomainEvent event, LocalDateTime executeTime) {
        return publishDelayedAt(event, executeTime, getDefaultStreamKey(event));
    }

    /**
     * 在指定时间点发布事件到指定Stream
     *
     * @param event 领域事件
     * @param executeTime 执行时间
     * @param streamKey 目标Stream键
     * @return 延迟任务ID
     */
    public String publishDelayedAt(DomainEvent event, LocalDateTime executeTime, String streamKey) {
        if (executeTime == null) {
            throw new IllegalArgumentException("执行时间不能为空");
        }

        long executeTimeMillis = executeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long delayMillis = executeTimeMillis - System.currentTimeMillis();

        if (delayMillis <= 0) {
            throw new IllegalArgumentException("执行时间必须在未来: " + executeTime);
        }

        return publishDelayed(event, delayMillis, streamKey);
    }

    /**
     * 取消延迟任务
     *
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    public boolean cancelDelayed(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return false;
        }

        // 尝试从时间轮取消
        boolean cancelled = timingWheelScheduler.cancel(taskId);

        if (!cancelled) {
            // TODO: 从SortedSet取消
        }

        return cancelled;
    }

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态，不存在返回null
     */
    public DelayedTask.TaskStatus getTaskStatus(String taskId) {
        // TODO: 实现状态查询
        return null;
    }

    /**
     * 获取待处理任务数
     *
     * @return 待处理任务数
     */
    public int getPendingTaskCount() {
        return timingWheelScheduler.getPendingTaskCount();
    }

    /**
     * 关闭发布器
     */
    public void shutdown() {
        scanner.shutdown();
        try {
            if (!scanner.awaitTermination(5, TimeUnit.SECONDS)) {
                scanner.shutdownNow();
            }
        } catch (InterruptedException e) {
            scanner.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 调度任务到合适的执行器
     */
    private String scheduleTask(DelayedTask task) {
        long delayMillis = task.getDelayMillis();

        if (delayMillis < SHORT_DELAY_THRESHOLD) {
            // 短延迟：使用时间轮
            boolean scheduled = timingWheelScheduler.schedule(task);
            if (scheduled) {
                return task.getId();
            }
        }

        // 长延迟或时间轮失败：使用SortedSet
        taskDispatcher.dispatchToSortedSet(task);
        return task.getId();
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 获取默认Stream键
     */
    private String getDefaultStreamKey(DomainEvent event) {
        // 从事件注解或类名推断Stream键
        return event.getEventType() + ":stream";
    }
}
