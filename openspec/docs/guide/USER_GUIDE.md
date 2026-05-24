# Redis工具集使用指南

> **版本**: v1.0.0  
> **更新日期**: 2026-05-24  
> **适用场景**: Java后端、Spring Boot应用、Redis集成

---

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.dekkerding</groupId>
    <artifactId>redis-toolkit-stream</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置Redis连接

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
```

### 3. 启用功能

```yaml
redis-toolkit:
  stream:
    event:
      enabled: true
      key-prefix: "stream:"
      partition:
        enabled: true
        count: 3
```

---

## 核心功能使用

### 1. Redis Stream事件发布

#### 1.1 定义事件

```java
@StreamEvent(streamKey = "order:stream", group = "order-consumers")
public class OrderCreatedEvent extends DomainEvent {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
}
```

#### 1.2 发布事件

```java
@Service
public class OrderService {
    @Autowired
    private EventPublishingService publishingService;
    
    public void createOrder(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .amount(order.getAmount())
                .build();
                
        MessageId messageId = publishingService.publish(event);
        log.info("订单事件已发布: messageId={}", messageId);
    }
}
```

#### 1.3 监听事件

```java
@Component
public class OrderEventHandler {
    
    @StreamEventListener(streamKey = "order:stream")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单事件: orderId={}", event.getOrderId());
        // 处理业务逻辑
    }
}
```

### 2. Pipeline批量发布（性能优化）

#### 2.1 配置Pipeline发布器

```java
@Bean
public EventPublisher eventPublisher(StringRedisTemplate redisTemplate) {
    // 使用Pipeline批量发布器，TPS提升3-5倍
    return new PipelineRedisStreamPublisher(redisTemplate, 100);
}
```

#### 2.2 批量发布

```java
@Autowired
private PipelineRedisStreamPublisher pipelinePublisher;

public void publishBatch(List<OrderCreatedEvent> events) {
    // 转换为EventEnvelope
    List<EventEnvelope> envelopes = events.stream()
            .map(event -> EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(event.getClass().getSimpleName())
                    .payload(event.toJson())
                    .build())
            .collect(Collectors.toList());
    
    // Pipeline批量发布
    List<MessageId> messageIds = pipelinePublisher.publishBatch(
            envelopes, 
            "order:stream"
    );
    
    log.info("批量发布完成: {}条消息", messageIds.size());
}
```

### 3. 消息压缩（自动）

#### 3.1 使用自适应压缩

```java
@Autowired
private AdaptiveCompressionStrategy compression;

public void publishWithCompression(OrderCreatedEvent event) {
    String payload = event.toJson();
    
    // 自动选择压缩算法
    AdaptiveCompressionStrategy.CompressedPayload compressed = 
            compression.compress(payload);
    
    // 发送压缩后的消息
    if (compressed.isCompressed()) {
        log.debug("消息已压缩: {}B→{}B", 
                payload.getBytes().length, compressed.getSize());
    }
}
```

#### 3.2 压缩效果

| 消息大小 | 压缩算法 | 压缩率 |
|---------|---------|--------|
| < 512B | 不压缩 | - |
| 512B-4KB | Snappy | 40-60% |
| \> 4KB | Gzip | 70-85% |

### 4. 对象池（性能优化）

#### 4.1 使用对象池

```java
@Autowired
private EventEnvelopePool envelopePool;

public void processWithPool() throws Exception {
    envelopePool.borrowAndUse(envelope -> {
        envelope.setEventId("test-123");
        envelope.setEventType("TestEvent");
        envelope.setPayload("{\\"data\\":\\"value\\"}");
        
        // 使用对象
        return processEnvelope(envelope);
    });
}
```

#### 4.2 查看池统计

```java
EventEnvelopePool.PoolStats stats = envelopePool.getStats();
log.info("对象池统计: {}", stats);
// 输出: PoolStats{active=2, idle=5, waiters=0, created=10, borrowed=50}
```

### 5. Redisson分布式集合

#### 5.1 分布式Map

```java
@Autowired
private RedissonFeatureService redissonService;

public void cacheUserInfo(String userId, UserInfo userInfo) {
    RMap<String, UserInfo> userCache = redissonService.getMap("user:cache");
    userCache.put(userId, userInfo);
    
    // 读取缓存
    UserInfo cached = userCache.get(userId);
}
```

#### 5.2 有序集合（排行榜）

```java
public void updateLeaderboard(String player, int score) {
    RScoredSortedSet<String> leaderboard = redissonService.getScoredSet("game:leaderboard");
    
    // 添加或更新分数
    leaderboard.add(score, player);
    
    // 获取Top 10
    Collection<String> top10 = leaderboard.valueRangeReversed(0, 9);
    log.info("排行榜前10: {}", top10);
}
```

### 6. 分布式队列

#### 6.1 延迟队列

```java
@Autowired
private RedissonFeatureService redissonService;

public void scheduleDelayedTask(String taskId, long delayMs) {
    RDelayedQueue<String> delayedQueue = redissonService.getDelayedQueue("task:delayed");
    
    // 延迟投递
    delayedQueue.offer(taskId, delayMs, TimeUnit.MILLISECONDS);
    log.info("任务已调度: taskId={}, delayMs={}ms", taskId, delayMs);
}
```

#### 6.2 消费延迟队列

```java
public void consumeDelayedTasks() {
    RBlockingQueue<String> queue = redissonService.getBlockingQueue("task:delayed");
    
    while (true) {
        String task = queue.poll(5, TimeUnit.SECONDS);
        if (task != null) {
            log.info("处理延迟任务: {}", task);
            // 处理任务
        }
    }
}
```

### 7. 原子操作

#### 7.1 分布式ID生成

```java
@Autowired
private RedissonFeatureService redissonService;

public String generateOrderId() {
    long id = redissonService.generateId("order");
    return "ORDER-" + id;
}
```

#### 7.2 布隆过滤器（去重）

```java
public boolean isDuplicate(String email) {
    RBloomFilter<String> bloomFilter = redissonService.getBloomFilter(
            "email:duplicate", 
            1_000_000,  // 预期100万条数据
            0.01       // 1%误判率
    );
    
    return bloomFilter.contains(email);
}
```

---

## 配置参考

### Pipeline配置

| 参数 | 默认值 | 说明 |
|------|-------|------|
| batchSize | 100 | 批量大小 |
| asyncThreads | CPU核心数 | 异步线程数 |

### 压缩配置

| 参数 | 阈值 | 压缩算法 |
|------|------|---------|
| < 512B | - | 不压缩 |
| 512B-4KB | - | Snappy |
| \> 4KB | - | Gzip |

### 对象池配置

| 参数 | 默认值 | 说明 |
|------|-------|------|
| maxTotal | 100 | 最大对象数 |
| maxIdle | 20 | 最大空闲数 |
| minIdle | 5 | 最小空闲数 |

---

## 性能基准

### 优化前后对比

| 指标 | 优化前 | 优化后 | 提升 |
|------|-------|-------|------|
| 小消息TPS | 4,000 | 12,000 | +200% |
| 中消息TPS | 1,500 | 4,500 | +200% |
| P99延迟 | 250ms | 80ms | -68% |
| GC频率 | 20次/分钟 | 12次/分钟 | -40% |

---

## 故障排查

### 常见问题

#### 1. Redis连接失败

```bash
# 检查Redis连接
redis-cli -h localhost -p 6379 ping

# 检查防火墙
telnet redis-server 6379
```

#### 2. Pipeline不生效

确保使用PipelineRedisStreamPublisher：

```java
@Bean
public EventPublisher eventPublisher(...) {
    return new PipelineRedisStreamPublisher(redisTemplate);
}
```

#### 3. 压缩导致性能下降

检查消息大小，小消息压缩反而降低性能。调整阈值：

```java
private static final int SNAPPY_THRESHOLD = 1024;  // 提高阈值
```

---

**文档维护**: Redis工具集团队
**版本**: v1.0.0
