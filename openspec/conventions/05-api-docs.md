# API文档规范

---

## 1. 公共API文档要求

### 1.1 JavaDoc注释规范

#### 公共接口必须包含完整JavaDoc

```java
/**
 * Redis分布式锁接口
 *
 * <p>提供跨JVM的互斥锁能力，基于Redis实现。
 * 支持可重入、读写锁、红锁、联锁等多种锁类型。
 *
 * <p>主要特性：
 * <ul>
 *   <li>原子性：基于Redis SET NX EX命令保证</li>
 *   <li>可重入：同一线程可多次获取同一把锁</li>
 *   <li>自动续期：支持看门狗机制自动延长锁持有时间</li>
 *   <li>超时释放：锁持有时间到期自动释放，避免死锁</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * Lock lock = new RedisLock(redisTemplate, properties);
 *
 * // 方式1：简单使用
 * if (lock.lock("my-resource", 30000)) {
 *     try {
 *         // 临界区代码
 *     } finally {
 *         lock.unlock("my-resource");
 *     }
 * }
 *
 * // 方式2：使用回调
 * lock.executeWithLock("my-resource", 30000, () -> {
 *     // 临界区代码
 * });
 * }</pre>
 *
 * @author Redis Toolkit Team
 * @version 1.0.0
 * @since 1.0.0
 * @see ReentrantLock 可重入锁实现
 * @see ReadWriteLock 读写锁实现
 * @see RedisLockException 锁相关异常
 */
public interface Lock {
    // ...
}
```

#### JavaDoc标签使用

| 标签 | 用途 | 示例 |
|------|------|------|
| `@param` | 参数说明 | `@param lockKey 锁的Key` |
| `@return` | 返回值说明 | `@return 是否成功获取锁` |
| `@throws` | 异常说明 | `@throws LockAcquireException 获取锁失败时抛出` |
| `@see` | 参考类/方法 | `@see ReentrantLock` |
| `@since` | 起始版本 | `@since 1.0.0` |
| `@author` | 作者 | `@author Redis Toolkit Team` |
| `@version` | 版本 | `@version 1.0.0` |
| `@deprecated` | 过期说明 | `@deprecated 请使用新方法` |

### 1.2 注解文档规范

```java
/**
 * Redis分布式锁注解
 *
 * <p>基于AOP的声明式锁，自动处理锁的获取和释放。
 * 支持SpEL表达式动态生成锁Key。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 示例1：简单使用
 * @RedisLock(key = "order:#{#orderId}", leaseTime = 30000)
 * public void processOrder(Long orderId) {
 *     // 业务逻辑
 * }
 *
 * // 示例2：使用EL表达式
 * @RedisLock(key = "'user:' + #user.id + ':lock'", leaseTime = 60000)
 * public void updateUser(User user) {
 *     // 业务逻辑
 * }
 *
 * // 示例3：可重入锁
 * @RedisLock(key = "resource:#{#resourceId}", lockType = LockType.REENTRANT)
 * public void processResource(String resourceId) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * <p>注意事项：
 * <ul>
 *   <li>锁的Key在方法执行完毕后自动释放</li>
 *   <li>方法执行异常时也会释放锁</li>
 *   <li>确保锁持有时间大于方法执行时间</li>
 *   <li>避免在锁内执行耗时操作</li>
 * </ul>
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 * @see Lock 锁接口
 * @see RedisLock Redis锁实现
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLock {

    /**
     * 锁的Key，支持SpEL表达式
     *
     * <p>SpEL上下文变量：
     * <ul>
     *   <li>{@code #参数名} - 方法参数</li>
     *   <li>{@code #root.methodName} - 方法名</li>
     *   <li>{@code #root.target} - 目标对象</li>
     * </ul>
     *
     * @return 锁的Key
     * @example "order:#{#orderId}"
     * @example "'user:' + #user.id + ':lock'"
     */
    @AliasFor("value")
    String key() default "";

    /**
     * 锁持有时间（毫秒）
     *
     * <p>默认30000ms（30秒），建议根据实际业务设置。
     * 过短可能导致业务未完成锁就释放，过长可能导致锁等待时间过长。
     *
     * @return 锁持有时间（毫秒）
     * @default 30000
     * @min 1000
     * @max 300000
     */
    long leaseTime() default 30000;

    /**
     * 获取锁的最大等待时间（毫秒）
     *
     * @return 等待时间（毫秒）
     * @default 10000
     * @min 0
     */
    long waitTime() default 10000;

    /**
     * 锁类型
     *
     * @return 锁类型
     * @default LockType.REENTRANT
     * @see LockType
     */
    LockType lockType() default LockType.REENTRANT;
}
```

### 1.3 配置属性文档规范

```java
/**
 * Redis工具包配置属性
 *
 * <p>全局配置示例：
 * <pre>{@code
 * redis-toolkit:
 *   enabled: true
 *   stream:
 *     event:
 *       enabled: true
 *       key-prefix: "stream:"
 *       partition:
 *         enabled: true
 *         count: 3
 *         strategy: hash
 *   lock:
 *     key-prefix: "lock:"
 *     default-lease-time: 30000
 *     default-wait-time: 10000
 * }</pre>
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "redis-toolkit")
@Data
@Validated
public class RedisToolkitProperties {

    /**
     * 是否启用Redis工具包
     *
     * @return true表示启用，false表示禁用
     * @default true
     */
    private boolean enabled = true;

    /**
     * Stream事件配置
     *
     * @return Stream配置属性
     */
    @Valid
    private Stream stream = new Stream();

    /**
     * 分布式锁配置
     *
     * @return 锁配置属性
     */
    @Valid
    private Lock lock = new Lock();

    /**
     * Stream配置
     */
    @Data
    public static class Stream {

        /**
         * 是否启用Stream功能
         *
         * @return true表示启用
         * @default true
         */
        private boolean enabled = true;

        /**
         * Stream Key前缀
         *
         * <p>所有Stream Key都会添加此前缀。
         *
         * @return Key前缀
         * @default "stream:"
         */
        private String keyPrefix = "stream:";

        /**
         * 默认消费者组名
         *
         * @return 消费者组名
         * @default "default-group"
         */
        private String defaultGroup = "default-group";

        /**
         * 轮询超时时间（毫秒）
         *
         * <p>XREADGROUP的超时时间，建议2000ms。
         *
         * @return 超时时间（毫秒）
         * @min 100
         * @max 60000
         * @default 2000
         */
        @Min(100)
        @Max(60000)
        private long pollTimeout = 2000;

        /**
         * 批量消费大小
         *
         * <p>每次批量拉取的消息数量。
         *
         * @return 批量大小
         * @min 1
         * @max 1000
         * @default 10
         */
        @Min(1)
        @Max(1000)
        private int batchSize = 10;
    }
}
```

---

## 2. README文档规范

### 2.1 模块README结构

每个核心功能模块应包含README.md：

```markdown
# Redis Stream - Redis替代Kafka

## 简介

Redis Stream模块提供了基于Redis Stream的消息发布订阅功能，替代Kafka实现事件驱动架构。

## 功能特性

- ✅ 发布订阅：XADD/XREADGROUP命令
- ✅ 消费者组：支持多消费者负载均衡
- ✅ 分区功能：支持按策略分区
- ✅ 幂等消费：防止消息重复处理
- ✅ 重试机制：指数退避重试
- ✅ 死信队列：失败消息处理

## 快速开始

### 1. 添加依赖

\`\`\`xml
<dependency>
    <groupId>io.github.dekkerding</groupId>
    <artifactId>redis-toolkit-stream</artifactId>
    <version>1.0.0</version>
</dependency>
\`\`\`

### 2. 配置

\`\`\`yaml
redis-toolkit:
  stream:
    event:
      enabled: true
\`\`\`

### 3. 发布事件

\`\`\`java
@StreamEvent(streamKey = "order:stream")
public class OrderCreatedEvent extends DomainEvent {
    // ...
}
\`\`\`

### 4. 监听事件

\`\`\`java
@StreamEventListener(streamKey = "order:stream")
public void handle(OrderCreatedEvent event) {
    // ...
}
\`\`\`

## 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| redis-toolkit.stream.event.enabled | 是否启用 | true |
| redis-toolkit.stream.event.key-prefix | Key前缀 | "stream:" |
| redis-toolkit.stream.event.partition.enabled | 是否分区 | false |

## API文档

### EventPublishingService

\`\`\`java
// 发布事件
MessageId publish(DomainEvent event);
\`\`\`

### StreamEventListener

\`\`\`java
// 监听事件
@StreamEventListener(streamKey = "order:stream")
void handleEvent(EventType event);
\`\`\`

## 性能指标

| 指标 | 目标值 |
|------|--------|
| 发布吞吐量 | >10K TPS |
| 消费延迟 | <100ms P99 |

## 注意事项

1. 确保Redis版本 >= 5.0
2. 生产环境建议启用分区功能
3. 合理设置poll-timeout避免CPU空转
```

### 2.2 配置属性参考文档

```markdown
## 配置属性参考

### Stream配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `redis-toolkit.stream.event.enabled` | Boolean | true | 是否启用Stream功能 |
| `redis-toolkit.stream.event.key-prefix` | String | "stream:" | Stream Key前缀 |
| `redis-toolkit.stream.event.default-group` | String | "default-group" | 默认消费者组名 |
| `redis-toolkit.stream.event.poll-timeout` | Long | 2000 | 轮询超时(ms) |
| `redis-toolkit.stream.event.batch-size` | Integer | 10 | 批量大小 |
| `redis-toolkit.stream.event.partition.enabled` | Boolean | false | 是否启用分区 |
| `redis-toolkit.stream.event.partition.count` | Integer | 3 | 分区数量 |
| `redis-toolkit.stream.event.partition.strategy` | String | "hash" | 分区策略 |

### Lock配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `redis-toolkit.lock.key-prefix` | String | "lock:" | 锁Key前缀 |
| `redis-toolkit.lock.default-lease-time` | Long | 30000 | 默认持有时间(ms) |
| `redis-toolkit.lock.default-wait-time` | Long | 10000 | 默认等待时间(ms) |
```

---

## 3. 使用示例文档

### 3.1 快速示例

```markdown
## 快速示例

### 发布消息

\`\`\`java
@Autowired
private EventPublishingService publishingService;

public void createOrder(Order order) {
    // 创建订单事件
    OrderCreatedEvent event = OrderCreatedEvent.builder()
        .orderId(order.getId())
        .amount(order.getAmount())
        .build();
    
    // 发布事件
    publishingService.publish(event);
}
\`\`\`

### 消费消息

\`\`\`java
@Component
public class OrderEventHandler {
    
    @StreamEventListener(streamKey = "order:stream")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单事件: {}", event.getOrderId());
        // 处理业务逻辑
    }
}
\`\`\`

### 启用分区

\`\`\`yaml
redis-toolkit:
  stream:
    event:
      partition:
        enabled: true
        count: 3
        strategy: hash
\`\`\`

分区路由示例：
\`\`\`java
// 相同orderId总是路由到同一分区
OrderCreatedEvent event1 = new OrderCreatedEvent("order-123");
OrderCreatedEvent event2 = new OrderCreatedEvent("order-123");
// 两个事件都会发布到: order:stream:1
\`\`\`
```

### 3.2 高级用法

```markdown
## 高级用法

### 自定义序列化

\`\`\`java
@Bean
@Primary
public EventSerializer customSerializer() {
    return new ProtobufEventSerializer();
}
\`\`\`

### 自定义分区策略

\`\`\`java
public class CustomPartitionStrategy implements PartitionStrategy {
    @Override
    public int partition(String key, int partitionCount) {
        // 自定义分区逻辑
        return customLogic(key) % partitionCount;
    }
}

@Bean
public PartitionStrategy customStrategy() {
    return new CustomPartitionStrategy();
}
\`\`\`

### 自定义重试策略

\`\`\`java
@Bean
public RetryPolicy customRetryPolicy() {
    return RetryPolicy.builder()
        .maxAttempts(5)
        .initialInterval(1000)
        .backoffMultiplier(3.0)
        .maxInterval(60000)
        .build();
}
\`\`\`
```

---

## 4. 版本迁移文档

```markdown
## 版本迁移指南

### 0.x → 1.0.0

#### 破坏性变更

1. **包名变更**
   \`\`\`java
   // 0.x
   import io.github.dekkerding.stream.*;
   
   // 1.0.0
   import io.github.dekkerding.redis.core.stream.*;
   \`\`\`

2. **配置前缀变更**
   \`\`\`yaml
   # 0.x
   spring.redis.stream.event.enabled: true
   
   # 1.0.0
   redis-toolkit.stream.event.enabled: true
   \`\`\`

#### 迁移步骤

1. 更新依赖版本到1.0.0
2. 更新import语句
3. 更新配置文件
4. 重新编译运行

#### 新特性

- 支持分区功能
- 支持幂等消费
- 支持自定义序列化
```

---

## 5. 故障排查文档

```markdown
## 故障排查

### 常见问题

#### 1. 消息丢失

**症状**: 发布的消息没有被消费

**排查步骤**:
1. 检查Stream是否存在: `XINFO STREAM stream:key`
2. 检查消费者组是否存在: `XINFO GROUPS stream:key`
3. 检查消费者状态: `XINFO CONSUMERS stream:key group-name`
4. 检查Pending消息: `XPENDING stream:key group-name - + -`

**解决方案**:
```bash
# 创建消费者组
XGROUP CREATE stream:key group-name 0 MKSTREAM

# 重置消费者
XGROUP SETID stream:key group-name 0
```

#### 2. 消费延迟

**症状**: 消息发布后很久才被消费

**可能原因**:
- poll-timeout设置过大
- 消费者处理慢
- Redis性能瓶颈

**解决方案**:
\`\`\`yaml
redis-toolkit:
  stream:
    event:
      poll-timeout: 2000  # 减小超时时间
      batch-size: 10      # 减少批量大小
\`\`\`

#### 3. 连接失败

**症状**: RedisConnectionException

**排查步骤**:
1. 检查Redis服务状态
2. 检查网络连通性
3. 检查密码配置

**解决方案**:
\`\`\`yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your-password
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
\`\`\`
```

---

**文档维护**: Redis工具集团队  
**更新频率**: 每次API变更后  
**下次审查**: 版本发布前