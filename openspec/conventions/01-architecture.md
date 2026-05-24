# Redis工具集架构规范

---

## 1. 工具库分层架构

**技术架构**: 【强制】采用工具库分层架构 + 模块化设计

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    业务应用层                                │
│              (引入redis-toolkit依赖)                          │
├─────────────────────────────────────────────────────────────┤
│              redis-toolkit-api (接口层)                      │
│              @StreamEvent, @EventListener等注解               │
├─────────────────────────────────────────────────────────────┤
│            redis-toolkit-core (核心实现层)                    │
│    Stream发布/消费、分布式锁、缓存、限流等核心功能              │
├─────────────────────────────────────────────────────────────┤
│          redis-toolkit-extension (扩展层)                    │
│      高级特性：监控、性能优化、多协议支持等                     │
├─────────────────────────────────────────────────────────────┤
│            redis-toolkit-common (公共层)                     │
│              工具类、常量、异常、序列化等                       │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 功能模块划分

| 模块 | 功能 | 说明 |
|------|------|------|
| **redis-stream** | Redis替代Kafka | 发布订阅、消费者组、分区、幂等、重试、DLQ |
| **redis-lock** | 分布式锁 | 可重入锁、读写锁、红锁、联锁 |
| **redis-cache** | 缓存封装 | 本地缓存+分布式缓存两级缓存 |
| **redis-rate-limit** | 限流 | 滑动窗口、令牌桶、漏桶 |
| **redis-id** | ID生成 | 雪花算法、序列号、唯一ID |
| **redis-delay-queue** | 延时队列 | 有序集合实现、定时轮询 |
| **redis-map** | 地理服务 | GEO命令封装 |
| **redis-hyperloglog** | 统计 | 基数统计、UV统计 |
| **redis-bloomfilter** | 布隆过滤器 | 误判判断、去重 |
| **redis-script** | Lua脚本 | 脚本加载、执行、缓存 |

### 1.3 模块依赖关系

```
redis-toolkit
├── redis-toolkit-common          (公共层 - 无依赖)
├── redis-toolkit-core            (依赖 common)
├── redis-toolkit-extension       (依赖 common, core)
└── redis-toolkit-api             (依赖 common, core)
    └── 自动配置类
```

**【强制】依赖规则:**
- 核心功能模块(core)之间禁止相互依赖
- 扩展模块(extension)可以依赖核心模块
- API层(api)可以暴露公共接口
- 公共层(common)禁止依赖任何业务模块

---

## 2. 技术栈选型

### 2.1 核心框架【强制要求】

| 技术 | 版本 | 说明 |
|------|------|------|
| **JDK 版本** | 8+ | 兼容性优先 |
| **Spring Boot** | 2.6+ | 自动配置支持 |
| **Lombok** | 1.18.30+ | 精简代码样板 |

### 2.2 Redis相关【强制要求】

| 技术 | 版本 | 说明 |
|------|------|------|
| **Spring Data Redis** | 2.6+ | Redis操作基础 |
| **Lettuce** | 6.2+ | 异步/响应式客户端 |
| **Jedis** | 4.3+ | 备选客户端 |
| **Redisson** | 3.16+ | 参考实现(不直接依赖) |
| **Spring Integration** | 5.5+ | 集成支持 |

### 2.3 工具库【强制要求】

| 依赖 | 版本 | 用途 |
|------|------|------|
| Guava | 30.0+ | 集合工具、缓存 |
| Hutool | 5.8+ | 通用工具库 |
| Commons Lang3 | 3.12+ | 字符串、对象工具 |
| Caffeine | 3.0+ | 高性能本地缓存 |
| Jackson | 2.15+ | JSON序列化 |

### 2.4 监控与测试【强制要求】

| 技术 | 版本 | 说明 |
|------|------|------|
| **Micrometer** | 1.9+ | 指标采集 |
| **Actuator** | 2.6+ | 健康检查 |
| **JUnit 5** | 5.8+ | 单元测试 |
| **Testcontainers** | 1.17+ | 集成测试 |
| **Awaitility** | 4.2+ | 异步测试等待 |

---

## 3. 模块内部包结构

### 3.1 公共模块(common)包结构

```
io.github.dekkerding.redis
├── annotation/         ← 注解定义
│   ├── RedisStream.java      ← Stream事件注解
│   ├── RedisLock.java        ← 锁注解
│   └── RedisCache.java       ← 缓存注解
├── constants/         ← 常量定义
│   ├── RedisConstants.java
│   └── CacheConstants.java
├── exception/         ← 异常定义
│   ├── RedisException.java   ← 基础异常
│   ├── LockAcquireException.java
│   └── SerializationException.java
├── model/             ← 数据模型
│   ├── EventEnvelope.java    ← 事件信封
│   ├── LockMetadata.java     ← 锁元数据
│   └── CacheEntry.java       ← 缓存条目
├── serializer/        ← 序列化
│   ├── Serializer.java       ← 序列化接口
│   ├── JsonSerializer.java
│   └── ProtostuffSerializer.java
└── utils/             ← 工具类
    ├── RedisKeyUtil.java
    └── ScriptLoader.java
```

### 3.2 核心模块(core)包结构

```
io.github.dekkerding.redis.core
├── stream/            ← Redis Stream功能
│   ├── publisher/
│   │   ├── StreamPublisher.java         ← 发布器接口
│   │   └── RedisStreamPublisher.java    ← Redis实现
│   ├── consumer/
│   │   ├── StreamConsumer.java          ← 消费者接口
│   │   └── RedisStreamConsumer.java     ← Redis实现
│   ├── registry/
│   │   └── ListenerRegistry.java        ← 监听器注册表
│   └── partition/
│       ├── PartitionStrategy.java       ← 分区策略接口
│       └── HashPartitionStrategy.java   ← 哈希实现
├── lock/              ← 分布式锁功能
│   ├── Lock.java                    ← 锁接口
│   ├── RedisLock.java               ← Redis实现
│   └── lockscript/                  ← Lua脚本
├── cache/             ← 缓存功能
│   ├── Cache.java                   ← 缓存接口
│   ├── RedisCache.java              ← Redis实现
│   └── loading/                     ← LoadingCache
├── rate/              ← 限流功能
│   ├── RateLimiter.java
│   └── impl/
├── id/                ← ID生成
│   ├── IdGenerator.java
│   └── SnowflakeIdGenerator.java
└── delay/             ← 延时队列
    ├── DelayQueue.java
    └── RedisDelayQueue.java
```

### 3.3 扩展模块(extension)包结构

```
io.github.dekkerding.redis.extension
├── monitor/           ← 监控扩展
│   ├── MetricsCollector.java
│   └── HealthIndicator.java
├── performance/       ← 性能优化
│   ├── PipelineExecutor.java
│   └── BatchExecutor.java
└── trace/             ← 链路追踪
    └── TraceInterceptor.java
```

### 3.4 API模块包结构

```
io.github.dekkerding.redis
├── autoconfigure/     ← 自动配置
│   ├── RedisToolkitAutoConfiguration.java
│   └── RedisToolkitProperties.java
├── bootstrap/          ← 启动引导
│   └── RedisToolkitBootstrap.java
└── enable/             ← 启用注解
    └── EnableRedisToolkit.java
```

---

## 4. 设计原则

### 4.1 API设计原则

| 原则 | 说明 | 示例 |
|------|------|------|
| **简单性** | API简洁易用 | `redisLock.lock()` 而非复杂配置 |
| **一致性** | 命名和风格统一 | 所有模块都用相似的方法签名 |
| **可扩展** | 支持自定义扩展 | 可替换序列化、分区策略等 |
| **性能优先** | 高性能实现 | Pipeline、批量、本地缓存 |

### 4.2 模块设计原则

| 原则 | 说明 |
|------|------|
| **职责单一** | 每个模块只负责一个功能领域 |
| **接口隔离** | 定义清晰的接口，隐藏实现细节 |
| **依赖倒置** | 依赖接口而非具体实现 |
| **开闭原则** | 对扩展开放，对修改关闭 |

---

## 5. 关键设计模式

### 5.1 策略模式

**应用场景**: 分区策略、序列化策略、限流算法

```java
// 接口定义
public interface PartitionStrategy {
    int partition(String key, int partitionCount);
}

// 具体实现
public class HashPartitionStrategy implements PartitionStrategy {
    @Override
    public int partition(String key, int partitionCount) {
        return Math.abs(key.hashCode()) % partitionCount;
    }
}

// 使用
PartitionStrategy strategy = PartitionStrategyFactory.getStrategy("hash");
int partition = strategy.partition("order-123", 3);
```

### 5.2 模板方法模式

**应用场景**: Redis操作的基础流程

```java
public abstract class AbstractRedisOperation<T> {
    public T execute(String key) {
        // 1. 序列化
        byte[] serializedKey = serialize(key);
        // 2. 执行Redis命令
        byte[] result = doExecute(serializedKey);
        // 3. 反序列化
        return deserialize(result);
    }

    protected abstract byte[] doExecute(byte[] key);
}
```

### 5.3 工厂模式

**应用场景**: 各种组件的创建

```java
public class RedisClientFactory {
    public static RedisClient createClient(RedisConfig config) {
        switch (config.getType()) {
            case LETTUCE:
                return new LettuceRedisClient(config);
            case JEDIS:
                return new JedisRedisClient(config);
            default:
                throw new IllegalArgumentException("Unsupported type");
        }
    }
}
```

### 5.4 包装器模式

**应用场景**: 功能增强（监控、缓存等）

```java
// 原始接口
public interface Lock {
    boolean lock();
    void unlock();
}

// 监控包装器
public class MonitoredLock implements Lock {
    private final Lock delegate;
    private final MetricsCollector collector;

    @Override
    public boolean lock() {
        long start = System.nanoTime();
        try {
            boolean result = delegate.lock();
            collector.recordSuccess(System.nanoTime() - start);
            return result;
        } catch (Exception e) {
            collector.recordFailure(System.nanoTime() - start);
            throw e;
        }
    }
}
```

---

## 6. 配置管理

### 6.1 配置结构

```yaml
redis-toolkit:
  enabled: true

  # Stream配置
  stream:
    event:
      enabled: true
      key-prefix: "stream:"
      default-group: "default-group"
      consumer-name: "${spring.application.name}"
      poll-timeout: 2000
      batch-size: 10
      partition:
        enabled: false
        count: 3
        strategy: "hash"
      idempotent:
        enabled: true
        key-ttl: 86400
      retry:
        max-attempts: 3
        initial-interval: 1000
        backoff-multiplier: 2.0
      dlq:
        enabled: true
        key-prefix: "dlq:"

  # 锁配置
  lock:
    default-lease-time: 30000
    default-wait-time: 10000
    lock-watchdog-timeout: 30000

  # 缓存配置
  cache:
    local:
      enabled: true
      maximum-size: 10000
      expire-after-write: 60000
    redis:
      enabled: true
      default-ttl: 3600

  # 限流配置
  rate-limit:
    default-permits: 100
    default-rate: 10
```

### 6.2 自动配置

```java
@Configuration
@EnableConfigurationProperties(RedisToolkitProperties.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisToolkitAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "redis-toolkit.stream.event", name = "enabled", havingValue = "true")
    public StreamEventPublisher streamEventPublisher(RedisTemplate<String, String> template) {
        return new RedisStreamPublisher(template);
    }

    // ... 其他Bean定义
}
```

---

## 7. 版本兼容性

### 7.1 Redis版本兼容

| 功能 | Redis最低版本 | 说明 |
|------|--------------|------|
| Stream | 5.0+ | XADD、XREADGROUP等 |
| GEO | 3.2+ | 地理位置命令 |
| HyperLogLog | 2.8.9+ | 基数统计 |
| Lua脚本 | 2.6+ | EVAL、SCRIPT LOAD |

### 7.2 Spring版本兼容

| Redis Toolkit版本 | Spring Boot版本 | Spring Data Redis版本 |
|------------------|----------------|---------------------|
| 1.0.x | 2.6.x | 2.6.x |
| 1.1.x | 2.7.x | 2.7.x |
| 2.0.x | 3.0.x | 3.0.x |

---

## 8. 性能基准

### 8.1 性能目标

| 功能 | 目标 | 测试条件 |
|------|------|---------|
| Stream发布 | >10K TPS | 单机、1KB消息 |
| Stream消费 | >10K TPS | 单消费者 |
| 分布式锁 | <10ms P99 | 无竞争 |
| 缓存GET | <1ms P99 | 本地+二级 |
| 限流判断 | <1ms P99 | 滑动窗口 |

### 8.2 性能优化策略

1. **Pipeline批量**: 减少网络往返
2. **本地缓存**: 减少Redis访问
3. **连接池**: 复用连接
4. **异步处理**: 提升吞吐量
5. **Lua脚本**: 保证原子性

---

## 9. 测试策略

### 9.1 测试金字塔

```
       E2E
      /   \
     /集成 \
    /   单元  \
   /__________\
```

| 测试类型 | 比例 | 说明 |
|---------|------|------|
| 单元测试 | 70% | JUnit + Mockito |
| 集成测试 | 20% | Testcontainers + Redis |
| E2E测试 | 10% | 完整流程验证 |

### 9.2 测试环境

```yaml
test:
  redis:
    mode: embedded  # embedded/remote/containers
    image: redis:7-alpine
```

---

**文档维护**: Redis工具集团队
**更新频率**: 每次架构变更后
**下次审查**: 季度架构审查