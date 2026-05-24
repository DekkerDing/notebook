package io.github.dekkerding.examples.application;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式ID生成器服务
 *
 * <p>基于Redisson实现的分布式ID生成器服务，提供生产级的ID生成功能。
 *
 * <p>核心功能：
 * <ul>
 *   <li>序列ID生成（自增）</li>
 *   <li>时间戳ID生成</li>
 *   <li>雪花算法ID生成</li>
 *   <li>业务ID生成（前缀+序列）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 简单序列ID
 * long id = idGeneratorService.nextId("order");
 *
 * // 业务ID（带前缀）
 * String orderId = idGeneratorService.nextBusinessId("ORD", "order");
 *
 * // 雪花算法ID
 * long snowflakeId = idGeneratorService.snowflakeId("order");
 *
 * // 时间戳ID
 * String timestampId = idGeneratorService.timestampId("txn");
 * }</pre>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class IdGeneratorService {

    @Autowired(required = false)
    private RedissonClient redisson;

    private static final String ID_KEY_PREFIX = "id:";

    /**
     * 默认初始值
     */
    private static final long DEFAULT_INIT_VALUE = 1L;

    /**
     * 默认步长
     */
    private static final long DEFAULT_STEP = 1L;

    // ==================== 简单序列ID ====================

    /**
     * 获取下一个ID（从1开始）
     *
     * @param key  ID序列Key（业务标识）
     * @return 下一个ID
     */
    public long nextId(String key) {
        return nextId(key, DEFAULT_INIT_VALUE);
    }

    /**
     * 获取下一个ID（指定初始值）
     *
     * @param key         ID序列Key（业务标识）
     * @param initValue   初始值（如果序列不存在）
     * @return 下一个ID
     */
    public long nextId(String key, long initValue) {
        assertRedissonAvailable();
        RAtomicLong atomicLong = getAtomicLong(key);

        // 如果序列不存在，设置初始值
        if (!atomicLong.isExists()) {
            atomicLong.set(initValue - 1);
        }

        long id = atomicLong.incrementAndGet();
        log.debug("生成序列ID: key={}, id={}", buildFullKey(key), id);
        return id;
    }

    /**
     * 批量获取ID（提升性能）
     *
     * @param key    ID序列Key（业务标识）
     * @param count  批量数量
     * @return 起始ID（后续ID为起始ID+1、起始ID+2...）
     */
    public long nextIdBatch(String key, int count) {
        assertRedissonAvailable();
        RAtomicLong atomicLong = getAtomicLong(key);

        if (!atomicLong.isExists()) {
            atomicLong.set(DEFAULT_INIT_VALUE - 1);
        }

        long startId = atomicLong.addAndGet(count);
        log.debug("批量生成序列ID: key={}, count={}, startId={}", buildFullKey(key), count, startId - count + 1);
        return startId - count + 1;
    }

    /**
     * 获取当前ID值（不自增）
     *
     * @param key  ID序列Key（业务标识）
     * @return 当前ID值
     */
    public long currentId(String key) {
        assertRedissonAvailable();
        RAtomicLong atomicLong = getAtomicLong(key);
        return atomicLong.get();
    }

    /**
     * 设置ID值
     *
     * @param key   ID序列Key（业务标识）
     * @param value ID值
     */
    public void setId(String key, long value) {
        assertRedissonAvailable();
        RAtomicLong atomicLong = getAtomicLong(key);
        atomicLong.set(value);
        log.debug("设置序列ID: key={}, value={}", buildFullKey(key), value);
    }

    // ==================== 业务ID（带前缀） ====================

    /**
     * 生成业务ID（前缀+日期+序列）
     *
     * <p>格式：{PREFIX}{yyyyMMdd}{序列号}
     * <br>示例：ORD20260524000001
     *
     * @param prefix  前缀（如：ORD、TXN、INV）
     * @param key     ID序列Key（业务标识）
     * @return 业务ID
     */
    public String nextBusinessId(String prefix, String key) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return nextBusinessId(prefix, key, dateStr);
    }

    /**
     * 生成业务ID（前缀+自定义后缀+序列）
     *
     * <p>格式：{PREFIX}{suffix}{序列号}
     * <br>示例：ORD20260524000001
     *
     * @param prefix  前缀（如：ORD、TXN、INV）
     * @param key     ID序列Key（业务标识）
     * @param suffix  自定义后缀（如：日期、月份等）
     * @return 业务ID
     */
    public String nextBusinessId(String prefix, String key, String suffix) {
        // 每天后缀重置序列
        String sequenceKey = key + ":" + suffix;
        long seq = nextId(sequenceKey, 1);

        // 格式化为8位序列号
        String businessId = String.format("%s%s%08d", prefix, suffix, seq);
        log.debug("生成业务ID: key={}, businessId={}", buildFullKey(sequenceKey), businessId);
        return businessId;
    }

    /**
     * 生成紧凑型业务ID（前缀+短序列）
     *
     * <p>格式：{PREFIX}{序列号}
     * <br>示例：ORD0000001
     *
     * @param prefix  前缀
     * @param key     ID序列Key（业务标识）
     * @return 业务ID
     */
    public String nextCompactId(String prefix, String key) {
        long seq = nextId(key, 1);
        return String.format("%s%07d", prefix, seq);
    }

    // ==================== 雪花算法ID ====================

    /**
     * 生成雪花算法ID
     *
     * <p>基于时间戳、机器ID、序列号生成分布式唯一ID。
     * 结构：1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号
     *
     * @param key  业务标识（用于隔离不同业务的机器ID）
     * @return 雪花算法ID
     */
    public long snowflakeId(String key) {
        // 使用简化版雪花算法
        long timestamp = System.currentTimeMillis();
        long machineId = getMachineId(key);
        long sequence = nextId("snowflake:" + key, 0) % 4096;

        long snowflakeId = ((timestamp - 1609459200000L) << 22)  // 时间戳偏移（2021-01-01）
                | (machineId << 12)                               // 机器ID（10位）
                | sequence;                                       // 序列号（12位）

        log.debug("生成雪花ID: key={}, id={}", key, snowflakeId);
        return snowflakeId;
    }

    /**
     * 获取机器ID
     *
     * <p>根据业务标识生成10位机器ID（0-1023）
     */
    private long getMachineId(String key) {
        // 简单hash生成机器ID
        int hash = key.hashCode();
        return Math.abs(hash) % 1024;
    }

    // ==================== 时间戳ID ====================

    /**
     * 生成时间戳ID
     *
     * <p>格式：{时间戳}{序列号}
     * <br>示例：20260524143052000001
     *
     * @param key  ID序列Key（业务标识）
     * @return 时间戳ID
     */
    public String timestampId(String key) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long seq = nextId("timestamp:" + key, 1);
        return String.format("%s%06d", timestamp, seq);
    }

    /**
     * 生成毫秒级时间戳ID
     *
     * <p>格式：{毫秒时间戳}{序列号}
     * <br>示例：1716553852000000001
     *
     * @param key  ID序列Key（业务标识）
     * @return 毫秒级时间戳ID
     */
    public long millisecondTimestampId(String key) {
        long timestamp = System.currentTimeMillis();
        long seq = nextId("millisecond:" + key, 0) % 10000;
        return timestamp * 10000 + seq;
    }

    // ==================== 预定义业务ID ====================

    /**
     * 生成订单ID
     *
     * <p>格式：ORD{yyyyMMdd}{序列号}
     *
     * @return 订单ID
     */
    public String orderId() {
        return nextBusinessId("ORD", "order");
    }

    /**
     * 生成支付ID
     *
     * <p>格式：PAY{yyyyMMdd}{序列号}
     *
     * @return 支付ID
     */
    public String paymentId() {
        return nextBusinessId("PAY", "payment");
    }

    /**
     * 生成退款ID
     *
     * <p>格式：REF{yyyyMMdd}{序列号}
     *
     * @return 退款ID
     */
    public String refundId() {
        return nextBusinessId("REF", "refund");
    }

    /**
     * 生成用户ID
     *
     * <p>格式：USR{yyyyMM}{序列号}
     *
     * @return 用户ID
     */
    public String userId() {
        String monthStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return nextBusinessId("USR", "user", monthStr);
    }

    /**
     * 生成交易ID
     *
     * <p>格式：TXN{yyyyMMddHHmmssSSS}{序列号}
     *
     * @return 交易ID
     */
    public String transactionId() {
        String millisecondStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return nextBusinessId("TXN", "transaction", millisecondStr);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取原子长整型对象
     */
    private RAtomicLong getAtomicLong(String key) {
        return redisson.getAtomicLong(buildFullKey(key));
    }

    /**
     * 构建完整的ID Key
     */
    private String buildFullKey(String key) {
        return ID_KEY_PREFIX + key;
    }

    /**
     * 断言Redisson可用
     */
    private void assertRedissonAvailable() {
        if (redisson == null) {
            throw new IllegalStateException("Redisson客户端未配置，ID生成器功能不可用");
        }
    }
}
