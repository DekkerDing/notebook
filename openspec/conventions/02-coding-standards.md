# Redis工具集编码规范

---

## 1. 包命名规范

- 【强制】包名层级：`io.github.dekkerding.redis.<module>.<component>`

**模块标识参考**：

```
io.github.dekkerding.redis
├── common                ← 公共模块
│   ├── annotation        ← 注解
│   ├── constants         ← 常量
│   ├── exception         ← 异常
│   ├── model             ← 数据模型
│   ├── serializer        ← 序列化
│   └── utils             ← 工具类
├── core                  ← 核心功能
│   ├── stream            ← Stream功能
│   ├── lock              ← 锁功能
│   ├── cache             ← 缓存功能
│   ├── rate              ← 限流功能
│   └── ...               ← 其他核心功能
├── extension            ← 扩展功能
│   ├── monitor           ← 监控
│   ├── performance       ← 性能优化
│   └── trace             ← 链路追踪
└── autoconfigure        ← 自动配置
```

【强制】命名规则：
- 全部使用小写，单词间用点号分隔
- 包名长度不超过 5 层（建议 3-4 层）
- 核心功能模块直接命名（stream/lock/cache），无需common后缀

---

## 2. 类命名规范

### 2.1 核心接口命名

| 接口类型 | 命名规范 | 示例 |
|---------|----------|------|
| 功能接口 | `<功能名>` | `StreamPublisher`, `Lock`, `Cache` |
| 回调接口 | `<功能名>Callback` | `LockCallback`, `MessageListener` |
| 工厂接口 | `<功能名>Factory` | `RedisClientFactory` |
| 策略接口 | `<功能名>Strategy` | `PartitionStrategy`, `RateLimitStrategy` |

### 2.2 实现类命名

| 实现类类型 | 命名规范 | 示例 |
|-----------|----------|------|
| Redis实现 | `<功能名>Redis<后缀>` | `RedisStreamPublisher`, `RedisLock` |
| 具体策略 | `<算法名><功能>Strategy` | `HashPartitionStrategy`, `TokenBucketRateLimitStrategy` |
| 包装器实现 | `<功能名><包装类型>Wrapper` | `MonitoredLockWrapper`, `CachedRedisWrapper` |

### 2.3 配置类命名

| 配置类类型 | 命名规范 | 示例 |
|-----------|----------|------|
| 配置属性 | `<功能名>Properties` | `StreamProperties`, `LockProperties` |
| 自动配置 | `<功能名>AutoConfiguration` | `RedisToolkitAutoConfiguration` |
| 条件配置 | `<功能名>Condition` | `OnStreamEnabledCondition` |

### 2.4 模型类命名

| 模型类类型 | 命名规范 | 示例 |
|---------|----------|------|
| 数据模型 | `<功能名>Model` | `LockMetadata`, `StreamMessage` |
| 事件模型 | `<功能名>Event` | `StreamEventPublishedEvent` |
| 枚举类 | `<功能名>Type` | `LockType`, `CacheType` |

### 2.5 工具类命名

| 工具类类型 | 命名规范 | 示例 |
|---------|----------|------|
| 工具类 | `<功能名>Util` | `RedisKeyUtil`, `ScriptLoaderUtil` |
| 帮助类 | `<功能名>Helper` | `ConnectionHelper`, `ScriptHelper` |

---

## 3. 注解定义规范

### 3.1 核心注解标准模板

```java
/**
 * Redis分布式锁注解
 *
 * <p>使用示例：
 * <pre>{@code
 * @RedisLock(key = "order:#{#orderId}", leaseTime = 30000)
 * public void processOrder(Long orderId) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLock {

    /**
     * 锁的Key，支持SpEL表达式
     */
    @AliasFor("value")
    String key() default "";

    /**
     * 锁的Key（别名）
     */
    @AliasFor("key")
    String value() default "";

    /**
     * 锁持有时间（毫秒），默认30秒
     */
    long leaseTime() default 30000;

    /**
     * 获取锁的最大等待时间（毫秒），默认10秒
     */
    long waitTime() default 10000;

    /**
     * 锁类型，默认可重入锁
     */
    LockType lockType() default LockType.REENTRANT;
}
```

### 3.2 注解命名规范

| 注解类型 | 命名规范 | 示例 |
|---------|----------|------|
| 功能注解 | `@Redis<功能名>` | `@RedisLock`, `@RedisCache` |
| 启用注解 | `@Enable<功能名>` | `@EnableRedisStream` |
| 条件注解 | `@On<条件>` | `@OnStreamEnabled` |

---

## 4. 接口设计规范

### 4.1 核心接口标准模板

```java
/**
 * Redis分布式锁接口
 *
 * <p>提供分布式锁的基本操作，支持：
 * <ul>
 *   <li>同步获取锁</li>
 *   <li>异步获取锁</li>
 *   <li>锁释放</li>
 *   <li>锁续期</li>
 *   <li>锁状态检查</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 * @see ReentrantLock 可重入锁实现
 * @see ReadWriteLock 读写锁实现
 */
public interface Lock {

    /**
     * 同步获取锁
     *
     * @param lockKey 锁的Key
     * @param leaseTime 锁持有时间（毫秒）
     * @return 是否成功获取锁
     * @throws LockAcquireException 获取锁失败时抛出
     */
    boolean lock(String lockKey, long leaseTime);

    /**
     * 带超时的同步获取锁
     *
     * @param lockKey 锁的Key
     * @param leaseTime 锁持有时间（毫秒）
     * @param waitTime 等待时间（毫秒）
     * @return 是否成功获取锁
     * @throws LockAcquireException 获取锁失败时抛出
     */
    boolean lock(String lockKey, long leaseTime, long waitTime);

    /**
     * 释放锁
     *
     * @param lockKey 锁的Key
     */
    void unlock(String lockKey);

    /**
     * 强制释放锁（忽略锁持有者检查）
     *
     * @param lockKey 锁的Key
     */
    void forceUnlock(String lockKey);

    /**
     * 检查锁是否被持有
     *
     * @param lockKey 锁的Key
     * @return 锁是否被持有
     */
    boolean isLocked(String lockKey);

    /**
     * 续期锁（延长锁持有时间）
     *
     * @param lockKey 锁的Key
     * @param leaseTime 新的锁持有时间（毫秒）
     * @return 是否续期成功
     */
    boolean renew(String lockKey, long leaseTime);
}
```

### 4.2 接口设计原则

| 原则 | 说明 | 正确示例 | 错误示例 |
|------|------|----------|----------|
| **简洁性** | 方法名简洁明了 | `lock(key)` | `acquireLockWithKey(key)` |
| **一致性** | 相似功能命名一致 | `lock/unlock`, `get/set` | `lock/acquire`, `unlock/release` |
| **可扩展** | 支持参数重载 | `lock(key)`, `lock(key, time)` | 只有复杂方法 |
| **异常明确** | 明确的异常类型 | `LockAcquireException` | `RuntimeException` |
| **文档完整** | 完整的JavaDoc | 包含使用示例 | 无文档 |

---

## 5. 实现类编码规范

### 5.1 实现类标准模板

```java
/**
 * Redis分布式锁实现
 *
 * <p>基于Redis SET NX EX命令实现，支持：
 * <ul>
 *   <li>原子性获取锁</li>
 *   <li>自动锁续期（看门狗）</li>
 *   <li>可重入支持</li>
 *   <li>Lua脚本保证原子性</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
public class RedisLock implements Lock {

    private final RedisTemplate<String, String> redisTemplate;
    private final LockProperties properties;
    private final LockWatcher lockWatcher;

    /**
     * 构造Redis分布式锁实例
     *
     * @param redisTemplate Redis模板
     * @param properties 锁配置属性
     * @param lockWatcher 锁监控器（看门狗）
     */
    public RedisLock(
            RedisTemplate<String, String> redisTemplate,
            LockProperties properties,
            LockWatcher lockWatcher) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.lockWatcher = lockWatcher;
    }

    @Override
    public boolean lock(String lockKey, long leaseTime) {
        return lock(lockKey, leaseTime, properties.getDefaultWaitTime());
    }

    @Override
    public boolean lock(String lockKey, long leaseTime, long waitTime) {
        Assert.hasText(lockKey, "Lock key must not be empty");
        Assert.isTrue(leaseTime > 0, "Lease time must be positive");
        Assert.isTrue(waitTime >= 0, "Wait time must be non-negative");

        String lockValue = generateLockValue();
        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    buildLockKey(lockKey),
                    lockValue,
                    leaseTime,
                    TimeUnit.MILLISECONDS
                );

                if (Boolean.TRUE.equals(acquired)) {
                    startWatchdog(lockKey, lockValue, leaseTime);
                    log.debug("Lock acquired: key={}, leaseTime={}ms", lockKey, leaseTime);
                    return true;
                }

                // 检查是否超时
                if (System.currentTimeMillis() - startTime > waitTime) {
                    log.debug("Lock acquire timeout: key={}, waitTime={}ms", lockKey, waitTime);
                    return false;
                }

                // 短暂休眠避免CPU空转
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquireException("Lock acquisition interrupted", e);
            } catch (Exception e) {
                throw new LockAcquireException("Failed to acquire lock: " + lockKey, e);
            }
        }
    }

    @Override
    public void unlock(String lockKey) {
        Assert.hasText(lockKey, "Lock key must not be empty");

        try {
            String script = buildUnlockScript();
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

            redisTemplate.execute(
                redisScript,
                Collections.singletonList(buildLockKey(lockKey)),
                generateLockValue()
            );

            stopWatchdog(lockKey);
            log.debug("Lock released: key={}", lockKey);
        } catch (Exception e) {
            throw new LockReleaseException("Failed to release lock: " + lockKey, e);
        }
    }

    // ... 其他方法实现

    /**
     * 构建锁的完整Key
     */
    private String buildLockKey(String lockKey) {
        return properties.getKeyPrefix() + lockKey;
    }

    /**
     * 生成锁值（包含唯一标识）
     */
    private String generateLockValue() {
        return UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
    }

    /**
     * 构建释放锁的Lua脚本
     */
    private String buildUnlockScript() {
        return "if redis.call('get', KEYS[1]) == ARGV[1] then " +
               "    return redis.call('del', KEYS[1]) " +
               "else " +
               "    return 0 " +
               "end";
    }
}
```

### 5.2 实现类编码规范

| 规范项 | 要求 | 说明 |
|--------|------|------|
| **日志** | 使用@Slf4j注解 | 禁止手动创建Logger |
| **依赖注入** | 使用@RequiredArgsConstructor | 配合final字段 |
| **参数校验** | 使用Assert或@Valid | 失败抛出IllegalArgumentException |
| **异常处理** | 转换为自定义异常 | 避免抛出原始异常 |
| **Lua脚本** | 使用预编译脚本 | 避免重复加载 |
| **资源清理** | 实现AutoCloseable(可选) | 用于资源释放 |

---

## 6. 配置类编码规范

### 6.1 配置属性类标准模板

```java
/**
 * Redis工具包配置属性
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "redis-toolkit")
@Data
@Validated
public class RedisToolkitProperties {

    /**
     * 是否启用Redis工具包
     */
    private boolean enabled = true;

    /**
     * Redis Stream配置
     */
    @Valid
    private Stream stream = new Stream();

    /**
     * 分布式锁配置
     */
    @Valid
    private Lock lock = new Lock();

    /**
     * 缓存配置
     */
    @Valid
    private Cache cache = new Cache();

    /**
     * Stream配置
     */
    @Data
    public static class Stream {

        /**
         * 是否启用Stream功能
         */
        private boolean enabled = true;

        /**
         * Stream Key前缀
         */
        private String keyPrefix = "stream:";

        /**
         * 默认消费者组名
         */
        private String defaultGroup = "default-group";

        /**
         * 轮询超时时间（毫秒）
         */
        @Min(100)
        @Max(60000)
        private long pollTimeout = 2000;

        /**
         * 批量消费大小
         */
        @Min(1)
        @Max(1000)
        private int batchSize = 10;
    }

    /**
     * 锁配置
     */
    @Data
    public static class Lock {

        /**
         * Key前缀
         */
        private String keyPrefix = "lock:";

        /**
         * 默认持有时间（毫秒）
         */
        @Min(1000)
        @Max(300000)
        private long defaultLeaseTime = 30000;

        /**
         * 默认等待时间（毫秒）
         */
        @Min(0)
        @Max(60000)
        private long defaultWaitTime = 10000;
    }

    /**
     * 缓存配置
     */
    @Data
    public static class Cache {

        /**
         * Key前缀
         */
        private String keyPrefix = "cache:";

        /**
         * 默认TTL（秒）
         */
        @Min(0)
        private long defaultTtl = 3600;

        /**
         * 本地缓存配置
         */
        @Valid
        private Local local = new Local();

        @Data
        public static class Local {

            /**
             * 是否启用本地缓存
             */
            private boolean enabled = true;

            /**
             * 最大缓存条目数
             */
            @Min(1)
            @Max(100000)
            private long maximumSize = 10000;

            /**
             * 写入后过期时间（毫秒）
             */
            @Min(0)
            private long expireAfterWrite = 60000;
        }
    }
}
```

### 6.2 自动配置类标准模板

```java
/**
 * Redis工具包自动配置
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(RedisToolkitProperties.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnProperty(prefix = "redis-toolkit", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RedisToolkitAutoConfiguration {

    private final RedisToolkitProperties properties;
    private final RedisTemplate<String, String> redisTemplate;

    public RedisToolkitAutoConfiguration(
            RedisToolkitProperties properties,
            RedisTemplate<String, String> redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    // ===== Stream配置 =====

    @Bean
    @ConditionalOnProperty(prefix = "redis-toolkit.stream", name = "enabled", havingValue = "true")
    public StreamEventPublisher streamEventPublisher() {
        log.info("Initializing StreamEventPublisher");
        return new RedisStreamPublisher(redisTemplate, properties.getStream());
    }

    // ===== 锁配置 =====

    @Bean
    @ConditionalOnMissingBean
    public Lock redisLock() {
        log.info("Initializing RedisLock");
        return new RedisLock(redisTemplate, properties.getLock());
    }

    // ===== 缓存配置 =====

    @Bean
    @ConditionalOnProperty(prefix = "redis-toolkit.cache.local", name = "enabled", havingValue = "true")
    public Cache redisCache() {
        log.info("Initializing RedisCache with local cache");
        return new TwoLevelCache(redisTemplate, properties.getCache());
    }
}
```

---

## 7. 异常处理规范

### 7.1 异常体系

```java
/**
 * Redis异常基类
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
```

### 7.2 错误码规范

| 错误码 | 说明 | HTTP状态码 |
|--------|------|-----------|
| `LOCK_ACQUIRE_FAILED` | 锁获取失败 | 409 |
| `LOCK_RELEASE_FAILED` | 锁释放失败 | 500 |
| `SERIALIZATION_ERROR` | 序列化错误 | 500 |
| `DESERIALIZATION_ERROR` | 反序列化错误 | 400 |
| `CACHE_MISS` | 缓存未命中 | 404 |
| `RATE_LIMIT_EXCEEDED` | 超出限流 | 429 |

---

## 8. 单元测试规范

### 8.1 测试类命名

```
<类名>Test.java        ← 单元测试
<类名>IntegrationTest.java  ← 集成测试
<功能>PerformanceTest.java  ← 性能测试
```

### 8.2 单元测试模板

```java
/**
 * RedisLock单元测试
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class RedisLockTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisLock redisLock;

    private LockProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LockProperties();
        properties.setKeyPrefix("test:");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("应该成功获取锁")
    void should_acquire_lock_when_key_is_available() {
        // Given
        String lockKey = "test-lock";
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
            .thenReturn(true);

        // When
        boolean result = redisLock.lock(lockKey, 30000);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).setIfAbsent(
            startsWith("test:test-lock"),
            anyString(),
            eq(30000L),
            eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("应该在超时后返回false")
    void should_return_false_when_acquire_timeout() {
        // Given
        String lockKey = "test-lock";
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
            .thenReturn(false);

        // When
        long waitTime = 100; // 短超时
        boolean result = redisLock.lock(lockKey, 30000, waitTime);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("应该抛出异常当Key为空")
    void should_throw_exception_when_key_is_empty() {
        // When & Then
        assertThatThrownBy(() -> redisLock.lock("", 30000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Lock key must not be empty");
    }
}
```

---

## 9. 文档注释规范

### 9.1 公共API文档要求

所有对外暴露的公共API（接口、公共类、公共方法）必须包含完整的JavaDoc：

```java
/**
 * 分布式锁接口
 *
 * <p>提供跨JVM的互斥锁能力，基于Redis实现。
 * 支持可重入、读写锁、红锁等多种锁类型。
 *
 * <p>使用示例：
 * <pre>{@code
 * Lock lock = new RedisLock(redisTemplate, properties);
 *
 * if (lock.lock("my-resource", 30000)) {
 *     try {
 *         // 临界区代码
 *     } finally {
 *         lock.unlock("my-resource");
 *     }
 * }
 * }</pre>
 *
 * @author Redis ToolKit Team
 * @version 1.0.0
 * @since 1.0.0
 * @see ReentrantLock 可重入锁实现
 * @see ReadWriteLock 读写锁实现
 */
public interface Lock {
    // ...
}
```

### 9.2 配置属性文档要求

配置属性必须包含@ApiModelProperty说明：

```java
@Data
@ConfigurationProperties(prefix = "redis-toolkit.lock")
public class LockProperties {

    /**
     * 锁的Key前缀
     *
     * @ApiModelProperty
     * value = "锁的Key前缀"
     * example = "lock:"
     * defaultValue = "lock:"
     */
    private String keyPrefix = "lock:";

    /**
     * 默认锁持有时间（毫秒）
     *
     * 最小值：1000，最大值：300000
     */
    @Min(1000)
    @Max(300000)
    private long defaultLeaseTime = 30000;
}
```

---

## 10. 性能优化规范

### 10.1 Pipeline使用规范

```java
/**
 * 批量操作使用Pipeline
 */
public class BatchRedisTemplate {

    public void batchSet(Map<String, String> data) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            data.forEach((key, value) -> {
                connection.set(key.getBytes(), value.getBytes());
            });
            return null;
        });
    }
}
```

### 10.2 本地缓存规范

```java
/**
 * 热数据本地缓存
 */
public class CachedRedisTemplate {

    private final Cache<String, String> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();

    public String get(String key) {
        return localCache.get(key, k -> {
            return redisTemplate.opsForValue().get(k);
        });
    }
}
```

---

**文档维护**: Redis工具集团队
**更新频率**: 每次规范变更后
**下次审查**: 季度规范审查