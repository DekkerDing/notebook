package io.github.dekkerding.examples.infrastructure.delayed;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延迟任务
 *
 * <p>封装需要延迟发送的领域事件，包含执行时间、目标Stream等信息。
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayedTask implements Delayed {

    /**
     * 任务唯一ID
     */
    private String id;

    /**
     * 领域事件
     */
    private DomainEvent event;

    /**
     * 目标Stream键
     */
    private String targetStream;

    /**
     * 执行时间（时间戳）
     */
    private long executeTimeMillis;

    /**
     * 创建时间
     */
    private long createTimeMillis;

    /**
     * 任务状态
     */
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 重试次数
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetry = 3;

    /**
     * 任务策略（时间轮或SortedSet）
     */
    @Builder.Default
    private ScheduleStrategy strategy = ScheduleStrategy.AUTO;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,    // 待执行
        SCHEDULED,  // 已调度
        EXECUTING,  // 执行中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        CANCELLED   // 已取消
    }

    /**
     * 调度策略枚举
     */
    public enum ScheduleStrategy {
        TIMING_WHEEL,  // 时间轮（短延迟 < 60s）
        SORTED_SET,    // SortedSet（长延迟 >= 60s）
        AUTO           // 自动选择
    }

    /**
     * 获取剩余延迟时间（毫秒）
     */
    @Override
    public long getDelay(TimeUnit unit) {
        long delay = executeTimeMillis - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 比较执行时间（用于优先队列）
     */
    @Override
    public int compareTo(Delayed other) {
        if (this == other) {
            return 0;
        }
        long diff = this.getDelay(TimeUnit.MILLISECONDS) -
                    other.getDelay(TimeUnit.MILLISECONDS);
        return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
    }

    /**
     * 是否已到期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= executeTimeMillis;
    }

    /**
     * 获取延迟时长（毫秒）
     */
    public long getDelayMillis() {
        return executeTimeMillis - createTimeMillis;
    }

    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetry;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount++;
    }
}
