package io.github.dekkerding.examples.infrastructure.pool;

import io.github.dekkerding.examples.domain.event.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.util.function.Function;

/**
 * EventEnvelope对象池
 *
 * <p>复用EventEnvelope对象，减少GC压力，预期GC频率降低40%
 *
 * <p>池化配置：
 * <ul>
 *   <li>最大对象数：100</li>
 *   <li>最大空闲数：20</li>
 *   <li>最小空闲数：5</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
public class EventEnvelopePool extends GenericObjectPool<EventEnvelope> {

    public EventEnvelopePool() {
        super(new EventEnvelopeFactory());
        setMaxTotal(100);      // 最大对象数
        setMaxIdle(20);        // 最大空闲数
        setMinIdle(5);         // 最小空闲数
        setTestOnBorrow(false); // 借出时不测试（性能优先）
        setTestOnReturn(false); // 归还时不测试
        log.info("EventEnvelope对象池已初始化");
    }

    /**
     * 借用并使用对象
     *
     * @param func 对象处理函数
     * @param <T> 返回类型
     * @return 处理结果
     * @throws Exception 处理异常
     */
    public <T> T borrowAndUse(Function<EventEnvelope, T> func) throws Exception {
        EventEnvelope envelope = borrowObject();
        try {
            return func.apply(envelope);
        } finally {
            returnObject(envelope);
        }
    }

    /**
     * 批量借用并使用对象
     *
     * @param count 借用数量
     * @param func 对象处理函数
     * @param <T> 返回类型
     * @return 处理结果列表
     * @throws Exception 处理异常
     */
    public <T> java.util.List<T> borrowBatchAndUse(int count, Function<EventEnvelope, T> func) throws Exception {
        java.util.List<T> results = new java.util.ArrayList<>(count);
        java.util.List<EventEnvelope> envelopes = new java.util.ArrayList<>(count);

        try {
            // 批量借用对象
            for (int i = 0; i < count; i++) {
                envelopes.add(borrowObject());
            }

            // 批量处理
            for (EventEnvelope envelope : envelopes) {
                results.add(func.apply(envelope));
            }

            return results;
        } finally {
            // 批量归还对象
            for (EventEnvelope envelope : envelopes) {
                returnObject(envelope);
            }
        }
    }

    /**
     * 获取池统计信息
     */
    public PoolStats getStats() {
        return new PoolStats(
                getNumActive(),
                getNumIdle(),
                getNumWaiters(),
                getCreatedCount(),
                getBorrowedCount()
        );
    }

    /**
     * 池统计信息
     */
    public static class PoolStats {
        private final int activeCount;
        private final int idleCount;
        private final int waitersCount;
        private final long createdCount;
        private final long borrowedCount;

        public PoolStats(int activeCount, int idleCount, int waitersCount, long createdCount, long borrowedCount) {
            this.activeCount = activeCount;
            this.idleCount = idleCount;
            this.waitersCount = waitersCount;
            this.createdCount = createdCount;
            this.borrowedCount = borrowedCount;
        }

        public int getActiveCount() {
            return activeCount;
        }

        public int getIdleCount() {
            return idleCount;
        }

        public int getWaitersCount() {
            return waitersCount;
        }

        public long getCreatedCount() {
            return createdCount;
        }

        public long getBorrowedCount() {
            return borrowedCount;
        }

        @Override
        public String toString() {
            return String.format("PoolStats{active=%d, idle=%d, waiters=%d, created=%d, borrowed=%d}",
                    activeCount, idleCount, waitersCount, createdCount, borrowedCount);
        }
    }

    /**
     * EventEnvelope对象工厂
     */
    public static class EventEnvelopeFactory extends BasePooledObjectFactory<EventEnvelope> {

        @Override
        public EventEnvelope create() {
            return EventEnvelope.builder().build();
        }

        @Override
        public PooledObject<EventEnvelope> wrap(EventEnvelope obj) {
            return new DefaultPooledObject<>(obj);
        }

        @Override
        public void passivateObject(PooledObject<EventEnvelope> p) {
            // 归还前清理数据
            p.getObject().reset();
        }

        @Override
        public void destroyObject(PooledObject<EventEnvelope> p) {
            p.getObject().destroy();
        }
    }
}
