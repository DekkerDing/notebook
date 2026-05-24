# 异常处理与日志规范

---

## 1. 异常处理规范

### 1.1 异常体系定义

#### RedisException 基础异常

```java
/**
 * Redis工具集异常基类
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
public class RedisException extends RuntimeException {

    private final String errorCode;

    public RedisException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RedisException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

#### 功能模块异常

```java
/**
 * 锁获取异常
 */
public class LockAcquireException extends RedisException {

    public LockAcquireException(String message) {
        super("LOCK_ACQUIRE_FAILED", message);
    }

    public LockAcquireException(String message, Throwable cause) {
        super("LOCK_ACQUIRE_FAILED", message, cause);
    }
}

/**
 * 序列化异常
 */
public class SerializationException extends RedisException {

    public SerializationException(String message) {
        super("SERIALIZATION_ERROR", message);
    }

    public SerializationException(String message, Throwable cause) {
        super("SERIALIZATION_ERROR", message, cause);
    }
}

/**
 * 连接异常
 */
public class RedisConnectionException extends RedisException {

    public RedisConnectionException(String message) {
        super("CONNECTION_ERROR", message);
    }

    public RedisConnectionException(String message, Throwable cause) {
        super("CONNECTION_ERROR", message, cause);
    }
}
```

### 1.2 错误码规范

#### 错误码格式

```
{模块}_{类型}_{编号}
```

| 模块 | 说明 |
|------|------|
| `LOCK` | 分布式锁模块 |
| `STREAM` | Stream模块 |
| `CACHE` | 缓存模块 |
| `RATE` | 限流模块 |
| `ID` | ID生成模块 |
| `SCRIPT` | Lua脚本模块 |

| 类型 | 说明 |
|------|------|
| `ACQUIRE` | 获取失败 |
| `RELEASE` | 释放失败 |
| `TIMEOUT` | 超时 |
| `INVALID` | 参数无效 |
| `ERROR` | 执行错误 |

#### 标准错误码定义

```java
/**
 * Redis工具集标准错误码
 */
public interface RedisErrorCode {

    String getCode();
    String getMessage();
}

/**
 * 通用错误码
 */
public enum CommonErrorCode implements RedisErrorCode {

    SUCCESS("0000", "成功"),
    INTERNAL_ERROR("9999", "系统内部错误"),
    PARAM_INVALID("1001", "参数无效"),
    CONNECTION_FAILED("1002", "Redis连接失败"),
    SERIALIZATION_ERROR("1003", "序列化失败");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

/**
 * Stream模块错误码
 */
public enum StreamErrorCode implements RedisErrorCode {

    CONSUMER_NOT_FOUND("2001", "消费者不存在"),
    GROUP_NOT_FOUND("2002", "消费者组不存在"),
    MESSAGE_EXPIRED("2003", "消息已过期"),
    PENDING_TOO_MANY("2004", "待处理消息过多");

    private final String code;
    private final String message;

    StreamErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

### 1.3 异常处理最佳实践

#### 在实现类中处理异常

```java
@Slf4j
public class RedisLock implements Lock {

    @Override
    public boolean lock(String lockKey, long leaseTime) {
        try {
            // Redis操作
            return doLock(lockKey, leaseTime);
        } catch (RedisConnectionException e) {
            // 连接异常直接抛出
            throw e;
        } catch (Exception e) {
            // 其他异常包装后抛出
            throw new LockAcquireException("Failed to acquire lock: " + lockKey, e);
        }
    }

    private boolean doLock(String lockKey, long leaseTime) {
        // 实际Redis操作
        // ...
    }
}
```

#### 重试机制

```java
/**
 * 带重试的Redis操作
 */
public class RetryableRedisTemplate {

    private final RedisTemplate<String, String> template;
    private final RetryPolicy retryPolicy;

    public <T> T executeWithRetry(RedisCallback<T> callback) {
        int attempts = 0;
        while (attempts < retryPolicy.getMaxAttempts()) {
            try {
                return callback.doInRedis();
            } catch (RedisConnectionException e) {
                attempts++;
                if (attempts >= retryPolicy.getMaxAttempts()) {
                    throw e;
                }
                long backoff = retryPolicy.getBackoff(attempts);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RedisException("OPERATION_INTERRUPTED", "Operation interrupted", ie);
                }
            }
        }
        throw new RedisException("RETRY_EXHAUSTED", "Retry attempts exhausted");
    }
}
```

---

## 2. 日志规范

### 2.1 日志级别使用

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| **ERROR** | 错误，需要立即处理 | Redis连接失败、锁获取失败 |
| **WARN** | 警告，不影响主流程 | 配置使用默认值、降级处理 |
| **INFO** | 关键操作记录 | 锁获取成功、消息发布成功 |
| **DEBUG** | 调试信息 | Redis命令执行细节 |
| **TRACE** | 详细跟踪 | Lua脚本执行详情 |

### 2.2 日志内容规范

#### 必须包含的信息

```java
// ✅ 正确：包含关键上下文
log.info("Lock acquired: key={}, leaseTime={}ms, holder={}", 
    lockKey, leaseTime, Thread.currentThread().getId());

log.error("Failed to publish message: stream={}, messageId={}, error={}", 
    streamKey, messageId, e.getMessage(), e);

// ❌ 错误：信息不足
log.info("Lock acquired");
log.error("Failed to publish message");
```

#### 日志格式模板

| 操作类型 | 日志模板 | 说明 |
|---------|---------|------|
| **Redis操作** | `{operation}: key={}, result={} | Redis操作结果 |
| **锁操作** | `Lock {}: key={}, holder={}, leaseTime={} | 锁状态变化 |
| **消息发布** | `Message published: stream={}, messageId={}, partition={} | 消息发布详情 |
| **消息消费** | `Message consumed: stream={}, messageId={}, consumer={} | 消息消费详情 |
| **错误** | `{operation} failed: key={}, error={}, cause={}` | 错误详情 |

### 2.3 日志脱敏规范

```java
/**
 * 敏感数据脱敏
 */
public class SensitiveDataMasker {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}(\\d{4})");

    public static String maskPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return phone;
        }
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    public static String maskIdCard(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            return idCard;
        }
        return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1********$2");
    }
}

// 使用示例
log.info("User info: phone={}, idCard={}", 
    SensitiveDataMasker.maskPhone(userInfo.getPhone()),
    SensitiveDataMasker.maskIdCard(userInfo.getIdCard()));
```

### 2.4 结构化日志

```java
/**
 * 结构化日志工具
 */
@Slf4j
public class StructuredLogger {

    public static void logLockOperation(String operation, String key, long leaseTime) {
        log.info("Lock operation: op={}, key={}, leaseTime={}, thread={}",
            operation, key, leaseTime, Thread.currentThread().getName());
    }

    public static void logStreamOperation(String operation, String stream, 
                                          String messageId, int partition) {
        log.info("Stream operation: op={}, stream={}, messageId={}, partition={}, thread={}",
            operation, stream, messageId, partition, Thread.currentThread().getName());
    }

    public static void logError(String operation, String key, Throwable e) {
        log.error("Operation failed: op={}, key={}, error={}, thread={}",
            operation, key, e.getMessage(), Thread.currentThread().getName(), e);
    }
}

// 使用示例
StructuredLogger.logLockOperation("acquire", "order:lock", 30000);
StructuredLogger.logError("publish", "order:stream", exception);
```

### 2.5 MDC上下文

```java
/**
 * MDC上下文管理
 */
public class MdcContextManager {

    private static final String TRACE_ID = "traceId";
    private static final String OPERATION = "operation";
    private static final String KEY = "key";

    public static void putTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void putOperation(String operation) {
        MDC.put(OPERATION, operation);
    }

    public static void putKey(String key) {
        MDC.put(KEY, key);
    }

    public static void clear() {
        MDC.clear();
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }
}

// 使用示例
@Override
public boolean lock(String lockKey, long leaseTime) {
    String traceId = UUID.randomUUID().toString();
    MdcContextManager.putTraceId(traceId);
    MdcContextManager.putOperation("lock:acquire");
    MdcContextManager.putKey(lockKey);

    try {
        boolean result = doLock(lockKey, leaseTime);
        log.info("Lock result: success={}, key={}", result, lockKey);
        return result;
    } finally {
        MdcContextManager.clear();
    }
}
```

---

## 3. 监控指标

### 3.1 关键指标定义

| 模块 | 指标名称 | 类型 | 说明 |
|------|---------|------|------|
| **Stream** | `redis.stream.publish.count` | Counter | 消息发布总数 |
| **Stream** | `redis.stream.consume.count` | Counter | 消息消费总数 |
| **Stream** | `redis.stream.publish.latency` | Timer | 发布延迟 |
| **Lock** | `redis.lock.acquire.count` | Counter | 锁获取次数 |
| **Lock** | `redis.lock.acquire.failed` | Counter | 锁获取失败次数 |
| **Lock** | `redis.lock.wait.time` | Timer | 等待时间 |
| **Lock** | `redis.lock.hold.time` | Timer | 持有时间 |
| **Cache** | `redis.cache.hit.count` | Counter | 缓存命中次数 |
| **Cache** | `redis.cache.miss.count` | Counter | 缓存未命中次数 |
| **Cache** | `redis.cache.latency` | Timer | 缓存操作延迟 |

### 3.2 指标收集示例

```java
/**
 * 指标收集服务
 */
@Service
@RequiredArgsConstructor
public class MetricsCollector {

    private final MeterRegistry registry;

    public void recordLockAcquire(String lockKey, boolean success, long waitTime) {
        Counter.builder("redis.lock.acquire.count")
            .tag("success", String.valueOf(success))
            .tag("key", maskKey(lockKey))
            .register(registry)
            .increment();

        if (success) {
            Timer.builder("redis.lock.wait.time")
                .tag("key", maskKey(lockKey))
                .register(registry)
                .record(waitTime, TimeUnit.MILLISECONDS);
        }
    }

    public void recordCacheAccess(String cacheKey, boolean hit) {
        Counter.builder("redis.cache.access.count")
            .tag("hit", String.valueOf(hit))
            .tag("key", maskKey(cacheKey))
            .register(registry)
            .increment();
    }

    private String maskKey(String key) {
        // 脱敏处理
        if (key.length() > 10) {
            return key.substring(0, 5) + "..." + key.substring(key.length() - 3);
        }
        return key;
    }
}
```

---

## 4. 异常处理模板

### 4.1 Redis操作通用模板

```java
/**
 * Redis操作通用模板
 */
public abstract class AbstractRedisOperation {

    protected final RedisTemplate<String, String> template;
    protected final MetricsCollector metrics;

    /**
     * 执行Redis操作，带异常处理和指标收集
     */
    protected <T> T execute(String operation, String key, RedisCallback<T> callback) {
        long startTime = System.nanoTime();
        try {
            T result = callback.doInRedis();
            
            // 记录成功指标
            recordSuccess(operation, key, startTime);
            return result;
            
        } catch (RedisConnectionException e) {
            // 连接异常直接抛出
            recordError(operation, key, "CONNECTION_ERROR", startTime);
            throw e;
            
        } catch (Exception e) {
            // 其他异常包装
            recordError(operation, key, e.getClass().getSimpleName(), startTime);
            throw new RedisException("OPERATION_ERROR", 
                operation + " failed: " + key, e);
        }
    }

    private void recordSuccess(String operation, String key, long startTime) {
        long latency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        Timer.builder("redis.operation.latency")
            .tag("operation", operation)
            .register(metrics.getRegistry())
            .record(latency, TimeUnit.MILLISECONDS);
    }

    private void recordError(String operation, String key, String error, long startTime) {
        Counter.builder("redis.operation.error.count")
            .tag("operation", operation)
            .tag("error", error)
            .register(metrics.getRegistry())
            .increment();
    }
}
```

---

**文档维护**: Redis工具集团队  
**更新频率**: 每次规范变更后  
**下次审查**: 季度规范审查