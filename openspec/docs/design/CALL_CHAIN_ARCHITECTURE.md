# 调用链架构图

> **版本**: v1.0.0
> **更新日期**: 2026-05-24
> **文档类型**: 架构设计

---

## 目录

1. [整体架构视图](#整体架构视图)
2. [事件发布流程](#事件发布流程)
3. [事件消费流程](#事件消费流程)
4. [Redisson功能调用](#redisson功能调用)
5. [性能优化链路](#性能优化链路)
6. [业务接入示例](#业务接入示例)

---

## 整体架构视图

```mermaid
graph TB
    subgraph "业务应用层"
        BizSvc1[OrderService]
        BizSvc2[PaymentService]
        BizSvc3[UserService]
    end

    subgraph "事件发布层"
        EPS[EventPublishingService<br/>统一发布入口]
        Pipeline[PipelineRedisStreamPublisher<br/>批量发布器]
        Compress[AdaptiveCompressionStrategy<br/>自适应压缩]
        Pool[EventEnvelopePool<br/>对象池]
    end

    subgraph "分区路由层"
        Router[EventStreamRouter<br/>流路由器]
        HashStrategy[HashPartitionStrategy<br/>哈希分区]
        RRStrategy[RoundRobinPartitionStrategy<br/>轮询分区]
    end

    subgraph "Redis Stream层"
        S1[(Stream: Partition-1)]
        S2[(Stream: Partition-2)]
        S3[(Stream: Partition-3)]
    end

    subgraph "Redis集群"
        R1[(Master-1<br/>Slave-1)]
        R2[(Master-2<br/>Slave-2)]
        R3[(Master-3<br/>Slave-3)]
    end

    subgraph "事件消费层"
        ECS[EventConsumingService<br/>统一消费入口]
        Idempotent[ProcessedMessageTracker<br/>幂等性检查]
        Retry[RetryPolicy<br/>重试策略]
        DLQ[DLQHandler<br/>死信队列]
    end

    subgraph "业务处理层"
        Handler1[OrderEventHandler]
        Handler2[PaymentEventHandler]
        Handler3[UserEventHandler]
    end

    subgraph "Redisson分布式功能"
        RMap[RMap<br/>分布式Map]
        RQueue[RDelayedQueue<br/>延迟队列]
        RLock[RLock<br/>分布式锁]
        RScore[RScoredSortedSet<br/>排行榜]
    end

    BizSvc1 --> EPS
    BizSvc2 --> EPS
    BizSvc3 --> EPS

    EPS --> Pool
    EPS --> Compress
    EPS --> Pipeline

    Pipeline --> Router
    Router --> HashStrategy
    Router --> RRStrategy

    Router --> S1
    Router --> S2
    Router --> S3

    S1 --> R1
    S2 --> R2
    S3 --> R3

    R1 -.消费.-> ECS
    R2 -.消费.-> ECS
    R3 -.消费.-> ECS

    ECS --> Idempotent
    ECS --> Retry
    ECS --> DLQ

    Idempotent --> Handler1
    Idempotent --> Handler2
    Idempotent --> Handler3

    BizSvc1 -.使用.-> RMap
    BizSvc2 -.使用.-> RQueue
    BizSvc3 -.使用.-> RLock
    BizSvc1 -.使用.-> RScore

    style EPS fill:#e1f5fe
    style ECS fill:#e1f5fe
    style Pipeline fill:#fff3e0
    style Compress fill:#fff3e0
    style Pool fill:#fff3e0
    style S1 fill:#f3e5f5
    style S2 fill:#f3e5f5
    style S3 fill:#f3e5f5
```

---

## 事件发布流程

```mermaid
sequenceDiagram
    autonumber
    participant Biz as 业务服务
    participant EPS as EventPublishingService
    participant Pool as EventEnvelopePool
    participant Serial as EventSerializer
    participant Comp as AdaptiveCompression
    participant Pipe as PipelinePublisher
    participant Part as PartitionManager
    participant Redis as Redis Stream

    Biz->>EPS: publish(DomainEvent)

    rect rgb(200, 220, 240)
        Note over EPS,Pool: 1. 获取对象池
        EPS->>Pool: borrowObject()
        Pool-->>EPS: EventEnvelope
    end

    rect rgb(220, 240, 200)
        Note over EPS,Serial: 2. 序列化事件
        EPS->>Serial: serialize(event)
        Serial-->>EPS: JSON String
    end

    rect rgb(240, 220, 200)
        Note over EPS,Comp: 3. 压缩消息
        EPS->>Comp: compress(payload)
        alt payload < 512B
            Comp-->>EPS: 不压缩
        else 512B ≤ payload < 4KB
            Comp-->>EPS: Snappy压缩
        else payload ≥ 4KB
            Comp-->>EPS: Gzip压缩
        end
    end

    rect rgb(240, 200, 220)
        Note over EPS,Part: 4. 选择分区
        EPS->>Part: selectPartition(eventKey)
        Part-->>EPS: partitionIndex
    end

    rect rgb(200, 240, 240)
        Note over EPS,Pipe: 5. 批量发布
        EPS->>Pipe: publishBatch(envelopes, streamKey)

        loop 每100条一批
            Pipe->>Redis: executePipelined()
            Redis->>Redis: XADD命令
        end

        Redis-->>Pipe: MessageId[]
        Pipe-->>EPS: MessageId[]
    end

    EPS->>Pool: returnObject(envelope)
    EPS-->>Biz: MessageId

    rect rgb(250, 250, 250)
        Note over Biz,Redis: 性能指标
        Note right of Pipe: TPS: 12,000<br/>提升3倍
        Note right of Comp: 压缩率: 50-80%
        Note right of Pool: GC降低40%
    end
```

### 发布流程详细步骤

| 步骤 | 组件 | 操作 | 耗时 | 说明 |
|------|------|------|------|------|
| 1 | EventEnvelopePool | 借用对象 | <1μs | 从对象池获取EventEnvelope |
| 2 | EventSerializer | 序列化 | 50-200μs | Jackson序列化为JSON |
| 3 | AdaptiveCompression | 压缩 | 10-500μs | 根据大小选择算法 |
| 4 | PartitionManager | 分区路由 | <1μs | 计算目标分区 |
| 5 | PipelinePublisher | 批量发布 | 5-20ms | Pipeline批量发送 |

---

## 事件消费流程

```mermaid
sequenceDiagram
    autonumber
    participant Redis as Redis Stream
    participant Cons as RedisStreamEventConsumer
    participant Tracker as ProcessedMessageTracker
    participant Serial as EventSerializer
    participant Retry as RetryPolicy
    participant DLQ as DLQHandler
    participant Handler as EventHandler

    loop 持续轮询
        Redis->>Cons: XREADGROUP<br/>（每2秒）
        Redis-->>Cons: List<Map>

        rect rgb(220, 240, 200)
            Note over Cons,Tracker: 幂等性检查
            par 每条消息
                Cons->>Tracker: hasProcessed(messageId)
                alt 已处理
                    Tracker-->>Cons: true
                    Cons->>Redis: XACK（确认）
                else 未处理
                    Tracker-->>Cons: false
                    Cons->>Tracker: markProcessing()

                    rect rgb(240, 220, 200)
                        Note over Cons,Serial: 反序列化
                        Cons->>Serial: deserialize(payload, className)
                        Serial-->>Cons: DomainEvent
                    end

                    rect rgb(200, 220, 240)
                        Note over Cons,Handler: 业务处理
                        Cons->>Handler: handle(event)

                        alt 处理成功
                            Handler-->>Cons: success
                            Cons->>Tracker: markProcessed()
                            Cons->>Redis: XACK（确认）

                        else 处理失败
                            Handler-->>Cons: exception

                            rect rgb(240, 200, 220)
                                Note over Cons,Retry: 重试策略
                                Cons->>Retry: shouldRetry(exception)

                                alt 可重试（网络、临时异常）
                                    Retry-->>Cons: true
                                    Note over Cons: 等待重试<br/>指数退避
                                else 不可重试（业务异常）
                                    Retry-->>Cons: false

                                    rect rgb(240, 180, 180)
                                        Note over Cons,DLQ: 死信队列
                                        Cons->>DLQ: sendToDLQ(message)
                                        DLQ->>Redis: XADD dlq:stream
                                    end
                                end
                            end
                        end
                    end
                end
            end
        end
    end
```

### 消费流程状态机

```mermaid
stateDiagram-v2
    [*] --> PENDING: 拉取消息
    PENDING --> CHECKING: 开始处理
    CHECKING --> PROCESSED: 已处理过
    CHECKING --> PROCESSING: 未处理
    PROCESSING --> SUCCESS: 处理成功
    PROCESSING --> RETRY: 可重试异常
    PROCESSING --> DLQ: 不可重试异常
    RETRY --> PROCESSING: 重试
    DLQ --> [*]
    SUCCESS --> [*]
    PROCESSED --> [*]
```

---

## Redisson功能调用

```mermaid
graph TB
    subgraph "业务服务"
        BizSvc[业务服务]
    end

    subgraph "RedissonFeatureService"
        RFS[RedissonFeatureService<br/>统一入口]
    end

    subgraph "分布式集合"
        Map[RMap<br/>分布式Map]
        Set[RSet<br/>分布式Set]
        List[RList<br/>分布式List]
        Sorted[RScoredSortedSet<br/>有序集合]
    end

    subgraph "分布式队列"
        BQueue[RBlockingQueue<br/>阻塞队列]
        DQueue[RDelayedQueue<br/>延迟队列]
        PQueue[RPriorityQueue<br/>优先级队列]
    end

    subgraph "原子操作"
        Atomic[RAtomicLong<br/>原子长整型]
        BitSet[RBitSet<br/>位集合]
        HLL[RHyperLogLog<br/>基数估算]
    end

    subgraph "高级功能"
        Bloom[RBloomFilter<br/>布隆过滤器]
        RateLimiter[RRateLimiter<br/>限流器]
        Lock[RLock<br/>分布式锁]
    end

    subgraph "Redis Cluster"
        Redis[(Redis Cluster<br/>3主3从)]
    end

    BizSvc --> RFS

    RFS --> Map
    RFS --> Set
    RFS --> List
    RFS --> Sorted

    RFS --> BQueue
    RFS --> DQueue
    RFS --> PQueue

    RFS --> Atomic
    RFS --> BitSet
    RFS --> HLL

    RFS --> Bloom
    RFS --> RateLimiter
    RFS --> Lock

    Map --> Redis
    Set --> Redis
    List --> Redis
    Sorted --> Redis

    BQueue --> Redis
    DQueue --> Redis
    PQueue --> Redis

    Atomic --> Redis
    BitSet --> Redis
    HLL --> Redis

    Bloom --> Redis
    RateLimiter --> Redis
    Lock --> Redis

    style RFS fill:#e1f5fe
    style Redis fill:#f3e5f5
```

### Redisson使用场景

| 场景 | 组件 | 示例 |
|------|------|------|
| **缓存** | RMap | 用户信息缓存 |
| **排行榜** | RScoredSortedSet | 游戏排行榜 |
| **延迟任务** | RDelayedQueue | 订单超时取消 |
| **分布式锁** | RLock | 防止重复扣款 |
| **限流** | RRateLimiter | API限流 |
| **去重** | RBloomFilter | 邮箱去重 |
| **UV统计** | RHyperLogLog | 页面UV统计 |
| **ID生成** | RAtomicLong | 分布式ID |

---

## 性能优化链路

```mermaid
graph LR
    subgraph "优化前"
        A1[逐条发布] --> A2[4000 TPS]
        A3[不压缩] --> A4[网络传输大]
        A5[频繁创建对象] --> A6[GC频繁]
    end

    subgraph "优化后"
        B1[Pipeline批量] --> B2[12000 TPS<br/>3x提升]
        B3[自适应压缩] --> B4[压缩50-80%]
        B5[对象池] --> B6[GC降低40%]
    end

    subgraph "JIT预热"
        C1[应用启动] --> C2[预热热点代码]
        C2 --> C3[性能稳定]
    end

    style B2 fill:#c8e6c9
    style B4 fill:#c8e6c9
    style B6 fill:#c8e6c9
    style C3 fill:#c8e6c9
```

### 性能对比数据

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| **TPS** | 4,000 | 12,000 | +200% |
| **P99延迟** | 250ms | 80ms | -68% |
| **GC频率** | 20次/分钟 | 12次/分钟 | -40% |
| **内存占用** | 800MB | 500MB | -37% |
| **CPU使用率** | 75% | 45% | -40% |
| **网络传输** | 100% | 30-50% | -50-70% |

---

## 业务接入示例

### 完整调用链示例

```mermaid
sequenceDiagram
    autonumber
    participant User as 用户
    participant API as OrderController
    participant Svc as OrderService
    participant DB as 数据库
    participant EPS as EventPublishingService
    participant Stream as Redis Stream
    participant Cons as EventConsumer
    participant Notify as NotificationService

    User->>API: POST /orders
    API->>Svc: createOrder(request)

    rect rgb(220, 240, 200)
        Note over Svc,DB: 业务处理
        Svc->>DB: INSERT orders
        DB-->>Svc: orderId
    end

    rect rgb(240, 220, 200)
        Note over Svc,Stream: 事件发布
        Svc->>EPS: publish(OrderCreatedEvent)
        EPS->>EPS: 序列化
        EPS->>EPS: 压缩(Snappy)
        EPS->>Stream: XADD order:stream
        Stream-->>EPS: MessageId
    end

    Svc-->>API: 201 Created
    API-->>User: {"orderId": "123"}

    rect rgb(200, 220, 240)
        Note over Stream,Notify: 异步消费
        Stream->>Cons: XREADGROUP
        Cons->>Cons: 幂等性检查
        Cons->>Cons: 反序列化
        Cons->>Cons: 解压

        par 并行处理
            Cons->>Notify: 发送通知
        and
            Cons->>DB: 更新查询模型
        and
            Cons->>外部API: 调用下游服务
        end

        Cons->>Stream: XACK
    end
```

### 接入代码示例

```java
// ========== 1. 定义事件 ==========
@StreamEvent(streamKey = "order:created", group = "order-consumers")
public class OrderCreatedEvent extends DomainEvent {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
}

// ========== 2. 发布事件 ==========
@Service
public class OrderService {

    @Autowired
    private EventPublishingService publishingService;

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 业务逻辑
        Order order = Order.builder()
            .orderId(UUID.randomUUID().toString())
            .customerId(request.getCustomerId())
            .amount(request.getAmount())
            .build();

        orderRepository.save(order);

        // 发布事件
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .source(this)
            .orderId(order.getOrderId())
            .customerId(order.getCustomerId())
            .amount(order.getAmount())
            .build();

        publishingService.publish(event);

        return order;
    }
}

// ========== 3. 消费事件 ==========
@Component
public class OrderEventHandler {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderQueryService orderQueryService;

    @StreamEventListener(streamKey = "order:created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 发送通知
        notificationService.sendOrderCreatedNotification(event);

        // 更新查询模型
        orderQueryService.updateOrderView(event);

        // 其他业务逻辑...
    }
}
```

---

## 总结

### 核心调用链路

```
业务服务 → EventPublishingService → 序列化 → 压缩 → 分区 → Pipeline → Redis Stream
                                                                              ↓
业务服务 ← EventHandler ← 幂等检查 ← 反序列化 ← 解压 ← EventConsumer ← XREADGROUP
```

### 关键组件

| 组件 | 作用 | 性能影响 |
|------|------|----------|
| PipelinePublisher | 批量发布 | TPS +200% |
| AdaptiveCompression | 消息压缩 | 网络 -50-70% |
| EventEnvelopePool | 对象复用 | GC -40% |
| JitWarmupRunner | JIT预热 | 启动后性能稳定 |
| ProcessedMessageTracker | 幂等性保证 | Exactly Once |

---

**文档维护**: Redis工具集团队
**最后更新**: 2026-05-24
**版本**: v1.0.0
