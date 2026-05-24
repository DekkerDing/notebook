# 可观测性规范

---

可观测性由**指标采集（Metrics）**、**结构化日志（Logging）**、**健康检查（Health）**三大支柱构成。

---

## 1. 指标采集规范

### 1.1 指标注册规范【强制】

- 【强制】使用 Spring Boot 内置的 `MeterRegistry`（Micrometer），禁止直接使用 Prometheus 原生 API
- 【强制】指标必须在 `@PostConstruct` 或配置类中预注册
- 【强制】所有自定义指标必须添加 `module` 和 `application` 基础标签

### 1.2 核心指标定义

| 指标名称 | 类型 | 标签 | 说明 |
|---------|------|------|------|
| `redis.stream.publish.count` | Counter | result, stream | 消息发布总数 |
| `redis.stream.consume.count` | Counter | result, stream | 消息消费总数 |
| `redis.stream.publish.latency` | Timer | stream | 发布延迟 |
| `redis.stream.consume.latency` | Timer | stream | 消费延迟 |
| `redis.lock.acquire.count` | Counter | result, key | 锁获取次数 |
| `redis.lock.hold.time` | Timer | key | 锁持有时间 |
| `redis.cache.hit.count` | Counter | key | 缓存命中次数 |
| `redis.cache.miss.count` | Counter | key | 缓存未命中次数 |

### 1.3 Stream指标收集器

```java
/**
 * Stream事件指标收集器
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@Component
public class StreamMetrics {

    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Timer publishTimer;
    private final Counter consumeSuccessCounter;
    private final Counter consumeFailureCounter;
    private final Timer consumeTimer;

    public StreamMetrics(MeterRegistry registry) {
        // 发布成功计数
        this.publishSuccessCounter = Counter.builder("redis.stream.publish.count")
            .tag("result", "success")
            .tag("module", "redis-toolkit")
            .description("Stream消息发布成功总数")
            .register(registry);

        // 发布失败计数
        this.publishFailureCounter = Counter.builder("redis.stream.publish.count")
            .tag("result", "failure")
            .tag("module", "redis-toolkit")
            .description("Stream消息发布失败总数")
            .register(registry);

        // 发布耗时
        this.publishTimer = Timer.builder("redis.stream.publish.latency")
            .tag("module", "redis-toolkit")
            .description("Stream消息发布耗时")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

        // 消费成功计数
        this.consumeSuccessCounter = Counter.builder("redis.stream.consume.count")
            .tag("result", "success")
            .tag("module", "redis-toolkit")
            .description("Stream消息消费成功总数")
            .register(registry);

        // 消费失败计数
        this.consumeFailureCounter = Counter.builder("redis.stream.consume.count")
            .tag("result", "failure")
            .tag("module", "redis-toolkit")
            .description("Stream消息消费失败总数")
            .register(registry);

        // 消费耗时
        this.consumeTimer = Timer.builder("redis.stream.consume.latency")
            .tag("module", "redis-toolkit")
            .description("Stream消息消费耗时")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    // 记录发布成功
    public void recordPublishSuccess(String stream, long durationMs) {
        publishSuccessCounter.increment();
        publishTimer.tag("stream", maskStream(stream)).record(durationMs, TimeUnit.MILLISECONDS);
    }

    // 记录发布失败
    public void recordPublishFailure(String stream, long durationMs) {
        publishFailureCounter.increment();
        publishTimer.tag("stream", maskStream(stream)).record(durationMs, TimeUnit.MILLISECONDS);
    }

    // 记录消费成功
    public void recordConsumeSuccess(String stream, long durationMs) {
        consumeSuccessCounter.increment();
        consumeTimer.tag("stream", maskStream(stream)).record(durationMs, TimeUnit.MILLISECONDS);
    }

    // 记录消费失败
    public void recordConsumeFailure(String stream, long durationMs) {
        consumeFailureCounter.increment();
        consumeTimer.tag("stream", maskStream(stream)).record(durationMs, TimeUnit.MILLISECONDS);
    }

    private String maskStream(String stream) {
        // 脱敏处理
        if (stream.length() > 20) {
            return stream.substring(0, 10) + "..." + stream.substring(stream.length() - 5);
        }
        return stream;
    }
}
```

### 1.4 分布式锁指标收集器

```java
/**
 * 分布式锁指标收集器
 */
@Component
public class LockMetrics {

    private final Counter acquireSuccessCounter;
    private final Counter acquireFailureCounter;
    private final Timer waitTimer;
    private final Timer holdTimer;

    public LockMetrics(MeterRegistry registry) {
        this.acquireSuccessCounter = Counter.builder("redis.lock.acquire.count")
            .tag("result", "success")
            .tag("module", "redis-toolkit")
            .description("分布式锁获取成功次数")
            .register(registry);

        this.acquireFailureCounter = Counter.builder("redis.lock.acquire.count")
            .tag("result", "failure")
            .tag("module", "redis-toolkit")
            .description("分布式锁获取失败次数")
            .register(registry);

        this.waitTimer = Timer.builder("redis.lock.wait.time")
            .tag("module", "redis-toolkit")
            .description("分布式锁等待时间")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

        this.holdTimer = Timer.builder("redis.lock.hold.time")
            .tag("module", "redis-toolkit")
            .description("分布式锁持有时间")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    public void recordAcquireSuccess(String key, long waitTimeMs) {
        acquireSuccessCounter.increment();
        waitTimer.tag("key", maskKey(key)).record(waitTimeMs, TimeUnit.MILLISECONDS);
    }

    public void recordAcquireFailure(String key) {
        acquireFailureCounter.increment();
    }

    public void recordHoldTime(String key, long holdTimeMs) {
        holdTimer.tag("key", maskKey(key)).record(holdTime, TimeUnit.MILLISECONDS);
    }

    private String maskKey(String key) {
        if (key.length() > 20) {
            return key.substring(0, 10) + "..." + key.substring(key.length() - 5);
        }
        return key;
    }
}
```

### 1.5 指标命名规范

| 规则 | 说明 | 示例 |
|------|------|------|
| 全小写，下划线分隔 | Prometheus 标准命名 | `redis_stream_publish_count` |
| 以模块名开头 | 便于 Grafana 过滤 | `redis_stream_xxx`, `redis_lock_xxx` |
| Counter 以 `_count` 结尾 | Prometheus 约定 | `redis_stream_publish_count` |
| Timer/Histogram 以 `_duration` 结尾 | 语义清晰 | `redis_stream_publish_duration` |

### 1.6 指标类型选择

| 指标类型 | 使用场景 | 典型示例 |
|---------|---------|---------|
| **Counter** | 只增不减的累计计数 | 发布总数、消费总数、锁获取次数 |
| **Timer** | 操作耗时分布 | 发布延迟、消费延迟、锁持有时间 |
| **Gauge** | 可升可降的当前值 | 连接池活跃连接数、待处理消息数 |

---

## 2. 日志规范

### 2.1 日志级别使用

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| **ERROR** | 错误，需要立即处理 | Redis连接失败、锁获取失败 |
| **WARN** | 警告，不影响主流程 | 配置使用默认值、重试 |
| **INFO** | 关键操作记录 | 消息发布成功、锁获取成功 |
| **DEBUG** | 调试信息 | Redis命令执行细节 |

### 2.2 结构化日志规范【强制】

```java
/**
 * 结构化日志工具
 */
@Slf4j
public class StructuredLogger {

    /**
     * 记录Stream操作日志
     */
    public static void logStreamOperation(String operation, String stream,
                                          String messageId, long duration) {
        log.info("Stream operation: op={}, stream={}, messageId={}, duration={}ms",
            operation, stream, messageId, duration);
    }

    /**
     * 记录锁操作日志
     */
    public static void logLockOperation(String operation, String key,
                                       long waitTime, long holdTime) {
        log.info("Lock operation: op={}, key={}, waitTime={}ms, holdTime={}ms",
            operation, key, waitTime, holdTime);
    }

    /**
     * 记录错误日志
     */
    public static void logError(String operation, String resource, Throwable e) {
        log.error("Operation failed: op={}, resource={}, error={}",
            operation, resource, e.getMessage(), e);
    }
}
```

### 2.3 日志内容规范

#### 必须包含的信息

```java
// ✅ 正确：包含关键上下文
log.info("Message published: stream={}, messageId={}, partition={}, duration={}ms",
    streamKey, messageId, partition, duration);

// ❌ 错误：信息不足
log.info("Message published");
```

#### 日志格式模板

| 操作类型 | 日志模板 | 说明 |
|---------|---------|------|
| **Stream发布** | `Message published: stream={}, messageId={}, partition={}` | 发布详情 |
| **Stream消费** | `Message consumed: stream={}, messageId={}, consumer={}` | 消费详情 |
| **锁操作** | `Lock {}: key={}, holder={}, leaseTime={}` | 锁状态变化 |
| **错误** | `{} failed: key={}, error={}, cause={}` | 错误详情 |

### 2.4 日志脱敏规范【强制】

```java
/**
 * 敏感数据脱敏工具
 */
public class SensitiveDataMasker {

    /**
     * 脱敏Key（保留前后部分）
     */
    public static String maskKey(String key) {
        if (StringUtils.isBlank(key) || key.length() <= 10) {
            return key;
        }
        return key.substring(0, 5) + "****" + key.substring(key.length() - 3);
    }

    /**
     * 脱敏消息内容
     */
    public static String maskMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return message;
        }
        return message.substring(0, Math.min(50, message.length())) + "...";
    }
}

// 使用示例
log.info("Message published: stream={}, messageId={}, content={}",
    streamKey, messageId, SensitiveDataMasker.maskMessage(content));
```

---

## 3. 健康检查规范

### 3.1 Actuator 端点配置【强制】

```yaml
management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  health:
    redis:
      enabled: true
    redisson:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      module: redis-toolkit
```

### 3.2 自定义健康检查

```java
/**
 * Redis Stream健康检查
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@Component("redisStreamHealthIndicator")
public class RedisStreamHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Health health() {
        try {
            // 检查Redis连接
            String ping = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            if ("PONG".equals(ping)) {
                // 检查Stream状态
                Map<String, Object> details = new HashMap<>();
                details.put("connection", "ok");
                details.put("streams", getStreamInfo());

                return Health.up()
                    .withDetails(details)
                    .build();
            }

            return Health.down()
                .withDetail("ping", ping)
                .build();

        } catch (Exception e) {
            return Health.down(e)
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private Map<String, Object> getStreamInfo() {
        // 获取Stream信息
        Map<String, Object> info = new HashMap<>();
        // TODO: 实现Stream信息采集
        return info;
    }
}
```

### 3.3 Redisson健康检查

```java
/**
 * Redisson健康检查
 */
@Component("redissonHealthIndicator")
public class RedissonHealthIndicator implements HealthIndicator {

    private final RedissonClient redissonClient;

    @Override
    public Health health() {
        try {
            // 检查Redisson连接
            boolean connected = redissonClient.getKeys()
                .count() >= 0;

            if (connected) {
                return Health.up()
                    .withDetail("connection", "ok")
                    .withDetail("nodes", getNodesInfo())
                    .build();
            }

            return Health.down()
                .withDetail("connection", "failed")
                .build();

        } catch (Exception e) {
            return Health.down(e)
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private Map<String, Object> getNodesInfo() {
        Map<String, Object> info = new HashMap<>();
        // TODO: 获取节点信息
        return info;
    }
}
```

---

## 4. 性能监控规范

### 4.1 慢操作监控

```java
/**
 * 慢操作监控器
 */
@Component
@Slf4j
public class SlowOperationMonitor {

    private static final long SLOW_THRESHOLD_MS = 1000;

    /**
     * 记录慢操作
     */
    public void recordSlowOperation(String operation, String resource,
                                   long durationMs) {
        if (durationMs > SLOW_THRESHOLD_MS) {
            log.warn("Slow operation detected: op={}, resource={}, duration={}ms",
                operation, resource, durationMs);

            // 记录慢操作指标
            Counter.builder("redis.slow.operation.count")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
        }
    }
}
```

### 4.2 连接池监控

```java
/**
 * 连接池监控
 */
@Component
@Slf4j
public class ConnectionPoolMonitor {

    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelay = 30000)
    public void monitorConnectionPool() {
        if (!(redisTemplate.getConnectionFactory() instanceof LettuceConnectionFactory)) {
            return;
        }

        LettuceConnectionFactory factory =
            (LettuceConnectionFactory) redisTemplate.getConnectionFactory();

        // 获取连接池指标
        int activeConnections = getActiveConnections(factory);
        int idleConnections = getIdleConnections(factory);
        int totalConnections = activeConnections + idleConnections;

        // 记录Gauge指标
        Gauge.builder("redis.pool.active.connections", () -> activeConnections)
            .register(meterRegistry);

        Gauge.builder("redis.pool.idle.connections", () -> idleConnections)
            .register(meterRegistry);

        Gauge.builder("redis.pool.total.connections", () -> totalConnections)
            .register(meterRegistry);

        // 告警检查
        if (activeConnections > 40) {
            log.warn("连接池活跃连接数过高: active={}", activeConnections);
        }
    }

    private int getActiveConnections(LettuceConnectionFactory factory) {
        // 实现获取活跃连接数
        return 0;
    }

    private int getIdleConnections(LettuceConnectionFactory factory) {
        // 实现获取空闲连接数
        return 0;
    }
}
```

---

## 5. 分布式追踪规范

### 5.1 TraceId传递

```java
/**
 * TraceId上下文管理
 */
public class TraceContext {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";

    /**
     * 设置TraceId
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * 获取TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        MDC.clear();
    }
}
```

### 5.2 事件发布追踪

```java
/**
 * 带追踪的事件发布
 */
@Service
@Slf4j
public class TracedEventPublishingService {

    private final EventPublishingService delegate;

    public MessageId publish(DomainEvent event) {
        // 生成TraceId
        String traceId = UUID.randomUUID().toString().replace("-", "");
        TraceContext.setTraceId(traceId);

        try {
            log.info("Publishing event: eventId={}, traceId={}",
                event.getEventId(), traceId);

            MessageId messageId = delegate.publish(event);

            log.info("Event published: eventId={}, messageId={}, traceId={}",
                event.getEventId(), messageId, traceId);

            return messageId;
        } finally {
            TraceContext.clear();
        }
    }
}
```

---

## 6. 告警阈值参考

| 指标 | 告警条件 | 严重级别 |
|------|---------|---------|
| 发布延迟 P99 | > 1000ms | WARNING |
| 消费延迟 P99 | > 2000ms | WARNING |
| 锁获取失败率 | > 5% | WARNING |
| 缓存命中率 | < 80% | WARNING |
| 连接池活跃率 | > 80% | WARNING |
| 慢操作数 | > 10/min | WARNING |
| Redis连接失败 | 连续 3 次 | CRITICAL |

---

## 7. 监控Dashboard规范

### 7.1 Grafana Dashboard变量

```json
{
  "variables": [
    {
      "name": "application",
      "type": "query",
      "query": "label_values(redis_stream_publish_count, application)"
    },
    {
      "name": "stream",
      "type": "query",
      "query": "label_values(redis_stream_publish_count, stream)"
    }
  ]
}
```

### 7.2 推荐图表

| 图表类型 | 查询 | 说明 |
|---------|------|------|
| 发布TPS | `rate(redis_stream_publish_count[5m])` | 发布速率 |
| 消费TPS | `rate(redis_stream_consume_count[5m])` | 消费速率 |
| 发布延迟P99 | `histogram_quantile(0.99, redis_stream_publish_latency)` | 发布延迟 |
| 消费延迟P99 | `histogram_quantile(0.99, redis_stream_consume_latency)` | 消费延迟 |
| 锁获取失败率 | `rate(redis_lock_acquire_count{result="failure"}[5m]) / rate(redis_lock_acquire_count[5m])` | 锁失败率 |

---

**文档维护**: Redis工具集团队
**更新频率**: 每次规范变更后
**下次审查**: 季度规范审查