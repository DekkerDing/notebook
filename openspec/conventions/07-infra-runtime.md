# 基础设施运行时规范

---

## 1. Redis连接管理

### 1.1 Lettuce客户端配置【强制】

```java
/**
 * Redis连接配置
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class RedisConnectionConfig {

    /**
     * StringRedisTemplate：Key和Value均为String
     * 适用场景：存JSON字符串、Token、验证码、计数器
     */
    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * 通用RedisTemplate（Key=String, Value=Object）
     * 适用场景：需要存储Java对象时
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
```

### 1.2 连接池配置规范

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| max-active | 50 | 最大活跃连接数 |
| max-idle | 20 | 最大空闲连接数 |
| min-idle | 5 | 最小空闲连接数 |
| max-wait | 1000ms | 获取连接最大等待时间 |

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
        max-wait: 1000ms
    shutdown-timeout: 100ms
```

---

## 2. Redisson分布式锁规范

### 2.1 Redisson客户端配置【强制】

```java
/**
 * Redisson分布式锁配置
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(RedissonClient.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedissonConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int database;

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer()
            .setAddress("redis://" + redisHost + ":" + redisPort)
            .setDatabase(database)
            .setConnectionMinimumIdleSize(5)
            .setConnectionPoolSize(20)
            .setConnectTimeout(5000)
            .setTimeout(10000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);

        if (StringUtils.hasText(redisPassword)) {
            serverConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}
```

### 2.2 锁命名规范

格式：`{应用前缀}:lock:{业务模块}:{资源标识}`

```
myapp:lock:order:123
myapp:lock:user:456
myapp:lock:stream:order:stream
```

### 2.3 标准使用模板【强制】

```java
/**
 * 分布式锁使用模板
 */
@Service
@Slf4j
public class LockTemplate {

    private final RedissonClient redissonClient;

    private static final long LOCK_WAIT_SECONDS  = 3L;
    private static final long LOCK_LEASE_SECONDS = 10L;

    /**
     * 执行带锁的操作
     */
    public <T> T executeWithLock(String lockKey, LockCallback<T> callback) {
        String fullKey = "myapp:lock:" + lockKey;
        RLock lock = redissonClient.getLock(fullKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw new LockAcquireException("获取锁失败: " + lockKey);
            }

            log.debug("锁获取成功: key={}, thread={}", fullKey, Thread.currentThread().getId());
            return callback.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquireException("锁获取中断: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("锁释放成功: key={}", fullKey);
            }
        }
    }

    @FunctionalInterface
    public interface LockCallback<T> {
        T execute();
    }
}
```

### 2.4 锁使用禁止行为

```
❌ 禁止 lock.lock() 无超时加锁（死锁风险）
❌ 禁止在finally中无条件调用 lock.unlock()
❌ 禁止锁粒度过大
❌ 禁止跨节点使用JVM锁替代分布式锁
❌ 禁止锁内执行耗时操作（RPC调用、批量处理）
```

---

## 3. 线程池规范

### 3.1 异步处理线程池配置

```java
/**
 * Redis工具包线程池配置
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(AsyncConfigurer.class)
public class AsyncThreadPoolConfig implements AsyncConfigurer {

    @Value("${redis-toolkit.async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${redis-toolkit.async.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${redis-toolkit.async.queue-capacity:200}")
    private int queueCapacity;

    @Bean("redisToolkitExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("redis-async-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

### 3.2 场景化线程池配置

| 场景 | 核心线程数 | 最大线程数 | 队列类型 | 拒绝策略 |
|------|-----------|-----------|---------|---------|
| Stream消息消费 | CPU×2 | CPU×4 | LinkedBlockingQueue(512) | CallerRunsPolicy |
| 异步事件发布 | 10 | 50 | LinkedBlockingQueue(200) | CallerRunsPolicy |
| 批量处理 | 5 | 10 | LinkedBlockingQueue(256) | DiscardOldestPolicy |

### 3.3 @Async使用规范【强制】

```java
/**
 * 异步事件发布服务
 */
@Service
@Slf4j
public class AsyncEventPublishingService {

    /**
     * 异步发布事件
     */
    @Async("redisToolkitExecutor")
    public CompletableFuture<MessageId> publishAsync(DomainEvent event) {
        try {
            MessageId messageId = doPublish(event);
            log.info("异步发布事件成功: eventId={}", event.getEventId());
            return CompletableFuture.completedFuture(messageId);
        } catch (Exception e) {
            log.error("异步发布事件失败: eventId={}", event.getEventId(), e);
            throw new EventPublishException("异步发布失败", e);
        }
    }
}
```

---

## 4. 缓存使用规范

### 4.1 Cache-Aside模式【强制】

```java
/**
 * Cache-Aside缓存模板
 */
@Service
@Slf4j
public class CacheTemplate {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String NULL_PLACEHOLDER = "NULL";
    private static final long NULL_CACHE_TTL = 60L;

    /**
     * 查询缓存
     */
    public <T> T get(String key, Class<T> type, CacheLoader<T> loader) {
        String cached = redisTemplate.opsForValue().get(key);

        // 空值缓存，防止缓存穿透
        if (NULL_PLACEHOLDER.equals(cached)) {
            return null;
        }

        if (cached != null) {
            return JSON.parseObject(cached, type);
        }

        // 回源加载
        T value = loader.load();
        if (value != null) {
            set(key, value, 600);
        } else {
            // 缓存空值
            redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, NULL_CACHE_TTL, TimeUnit.SECONDS);
        }

        return value;
    }

    /**
     * 设置缓存
     */
    public <T> void set(String key, T value, long ttlSeconds) {
        String json = JSON.toJSONString(value);
        redisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @FunctionalInterface
    public interface CacheLoader<T> {
        T load();
    }
}
```

### 4.2 缓存Key命名规范

格式：`{应用前缀}:{功能域}:{资源标识}`

```
myapp:cache:user:123
myapp:cache:product:456
myapp:cache:order:list:pending
```

### 4.3 缓存TTL规范

| 缓存类型 | 推荐TTL | 说明 |
|---------|---------|------|
| 用户信息 | 30分钟 | 低频变更 |
| 商品信息 | 10分钟 | 中频变更 |
| 订单状态 | 5分钟 | 高频变更 |
| 配置信息 | 60分钟 | 低频变更 |

### 4.4 缓存使用禁止行为

```
❌ 禁止在事务内直接删除/更新缓存（应在afterCommit后操作）
❌ 禁止缓存大对象（单个Value > 1MB）
❌ 禁止设置无TTL的缓存
❌ 禁止使用keys * 命令扫描
❌ 禁止在循环内逐条读写缓存
```

---

## 5. Stream消费者管理

### 5.1 消费者启动规范

```java
/**
 * Stream消费者启动器
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class StreamConsumerBootstrap implements ApplicationRunner {

    private final StreamConsumerRegistry consumerRegistry;
    private final RedisToolkitProperties properties;

    @Value("${redis-toolkit.stream.event.auto-start:true}")
    private boolean autoStart;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoStart) {
            log.info("Stream消费者自动启动已禁用");
            return;
        }

        log.info("启动Stream消费者...");
        consumerRegistry.startAllConsumers();
        log.info("Stream消费者启动完成");
    }
}
```

### 5.2 消费者线程池配置

```java
/**
 * Stream消费者线程池配置
 */
@Configuration
public class StreamConsumerConfig {

    @Bean("streamConsumerExecutor")
    public Executor streamConsumerExecutor() {
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("stream-consumer-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

---

## 6. 连接池监控

### 6.1 连接池指标采集

```java
/**
 * Redis连接池指标收集器
 */
@Component
@Slf4j
public class RedisPoolMetrics {

    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry registry;

    @PostConstruct
    public void collectMetrics() {
        if (!(redisTemplate.getConnectionFactory() instanceof LettuceConnectionFactory)) {
            return;
        }

        LettuceConnectionFactory factory = (LettuceConnectionFactory) redisTemplate.getConnectionFactory();

        Gauge.builder("redis.pool.active.connections", getPoolSize(factory))
            .tag("pool", "redis")
            .register(registry);

        Gauge.builder("redis.pool.idle.connections", getIdleCount(factory))
            .tag("pool", "redis")
            .register(registry);
    }

    private AtomicInteger getPoolSize(LettuceConnectionFactory factory) {
        // 获取活跃连接数
        return new AtomicInteger(0);
    }

    private AtomicInteger getIdleCount(LettuceConnectionFactory factory) {
        // 获取空闲连接数
        return new AtomicInteger(0);
    }
}
```

---

## 7. 资源清理规范

### 7.1 优雅关闭

```java
/**
 * Redis工具包优雅关闭处理器
 */
@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class RedisToolkitShutdownHandler {

    private final StreamConsumerRegistry consumerRegistry;

    @PreDestroy
    public void shutdown() {
        log.info("开始关闭Redis工具包...");

        try {
            // 停止消费者
            consumerRegistry.stopAllConsumers();
            log.info("Stream消费者已停止");
        } catch (Exception e) {
            log.error("停止Stream消费者失败", e);
        }

        log.info("Redis工具包关闭完成");
    }
}
```

### 7.2 资源释放检查

```java
/**
 * 连接泄漏检测器
 */
@Component
@Slf4j
public class ConnectionLeakDetector {

    @Scheduled(fixedDelay = 60000)
    public void detectConnectionLeak() {
        // 检测连接池状态
        // 如果连接数长时间异常，告警
    }
}
```

---

## 8. 故障恢复规范

### 8.1 连接异常重试

```java
/**
 * Redis操作重试模板
 */
@Component
@Slf4j
public class RedisRetryTemplate {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    public <T> T executeWithRetry(RedisCallback<T> callback) {
        int attempts = 0;
        RedisException lastException = null;

        while (attempts < MAX_RETRIES) {
            try {
                return callback.doInRedis();
            } catch (RedisConnectionException e) {
                lastException = e;
                attempts++;

                if (attempts >= MAX_RETRIES) {
                    break;
                }

                log.warn("Redis操作失败，准备重试: attempt={}/{}", attempts, MAX_RETRIES);

                try {
                    Thread.sleep(RETRY_DELAY_MS * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RedisException("OPERATION_INTERRUPTED", "操作被中断", ie);
                }
            }
        }

        throw new RedisException("RETRY_EXHAUSTED",
            "重试次数耗尽: " + MAX_RETRIES, lastException);
    }

    @FunctionalInterface
    public interface RedisCallback<T> {
        T doInRedis();
    }
}
```

### 8.2 断路器模式

```java
/**
 * Redis断路器
 */
@Component
@Slf4j
public class RedisCircuitBreaker {

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private volatile boolean circuitOpen = false;

    private static final int FAILURE_THRESHOLD = 5;
    private static final int SUCCESS_THRESHOLD = 3;
    private static final long TIMEOUT_MS = 60000;

    private long lastFailureTime = 0;

    public <T> T execute(Supplier<T> supplier, Supplier<T> fallback) {
        if (circuitOpen) {
            if (System.currentTimeMillis() - lastFailureTime > TIMEOUT_MS) {
                // 尝试半开状态
                circuitOpen = false;
                failureCount.set(0);
            } else {
                log.warn("断路器已打开，执行降级逻辑");
                return fallback.get();
            }
        }

        try {
            T result = supplier.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            return fallback.get();
        }
    }

    private void onSuccess() {
        int current = successCount.incrementAndGet();
        if (current >= SUCCESS_THRESHOLD) {
            circuitOpen = false;
            failureCount.set(0);
            successCount.set(0);
        }
    }

    private void onFailure() {
        int current = failureCount.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();

        if (current >= FAILURE_THRESHOLD) {
            circuitOpen = true;
            log.error("断路器已打开: failureCount={}", current);
        }
    }
}
```

---

**文档维护**: Redis工具集团队
**更新频率**: 每次规范变更后
**下次审查**: 季度规范审查