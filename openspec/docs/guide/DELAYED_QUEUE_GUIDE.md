# Redis Stream 延迟队列使用指南

> **版本**: v1.0.0
> **更新日期**: 2026-05-24
> **适用场景**: 订单超时取消、定时提醒、延迟处理等

---

## 目录

1. [快速开始](#快速开始)
2. [核心概念](#核心概念)
3. [业务接入](#业务接入)
4. [使用示例](#使用示例)
5. [最佳实践](#最佳实践)

---

## 快速开始

### 1. 添加依赖

延迟队列功能已集成到 `redis-toolkit-stream` 模块中，无需额外依赖。

### 2. 启用功能

```yaml
# application.yml
redis-toolkit:
  delayed:
    enabled: true
    timing-wheel:
      tick-duration: 100  # 时间刻度(ms)
      wheel-size: 600      # 时间轮大小
    sorted-set:
      scan-interval: 1000  # 扫描间隔(ms)
```

### 3. 注入使用

```java
@Service
public class OrderService {

    @Autowired
    private DelayedEventPublisher delayedPublisher;

    public void createOrder(Order order) {
        // 延迟30分钟发送取消提醒
        delayedPublisher.publishDelayed(
            OrderCancelEvent.of(order),
            30 * 60 * 1000  // 30分钟
        );
    }
}
```

---

## 核心概念

### 混合架构

延迟队列采用**时间轮 + SortedSet**混合架构：

```
┌─────────────────────────────────────────┐
│         DelayedEventPublisher            │
│         (业务接入API)                     │
└────────────────┬────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
┌──────────────┐  ┌──────────────┐
│ 时间轮调度器   │  │ SortedSet    │
│ (短延迟<60s)  │  │ (长延迟≥60s) │
│ • O(1)插入    │  │ • Redis持久化 │
│ • 低CPU占用   │  │ • 支持重启恢复│
└──────┬───────┘  └──────┬───────┘
       │                 │
       └────────┬────────┘
                ▼
      ┌─────────────────┐
      │ DelayedTask     │
      │ Dispatcher      │
      └────────┬────────┘
               ▼
      ┌─────────────────┐
      │ Redis Stream    │
      └─────────────────┘
```

### 时间轮算法

```
时间轮结构（精度100ms，60秒一圈）

     0ms    100ms  200ms        59900ms
   ┌────┐ ┌────┐ ┌────┐ ...  ┌────┐
   │ T1 │ │    │ │ T2 │      │ T3 │
   └────┘ └────┘ └────┘      └────┘
     ↑
   tick

每100ms移动一次指针
触发桶中所有到期任务
```

---

## 业务接入

### API设计

```java
public interface DelayedEventPublisher {

    // 延迟N毫秒后发布
    String publishDelayed(DomainEvent event, long delayMillis);

    // 在指定时间点发布
    String publishDelayedAt(DomainEvent event, LocalDateTime executeTime);

    // 取消延迟任务
    boolean cancelDelayed(String taskId);

    // 查询任务状态
    DelayedTask.TaskStatus getTaskStatus(String taskId);
}
```

### 无感接入

**设计原则**: API与普通发布保持一致，仅增加延迟参数

```java
// ========== 之前：普通发布 ==========
publishingService.publish(event);

// ========== 之后：延迟发布 ==========
delayedPublisher.publishDelayed(event, 5000);  // 仅添加延迟参数
delayedPublisher.publishDelayedAt(event, time); // 或指定时间
```

---

## 使用示例

### 场景1: 订单超时取消

```java
@Service
public class OrderService {

    @Autowired
    private DelayedEventPublisher delayedPublisher;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 创建订单 - 30分钟后自动取消未支付订单
     */
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .status(OrderStatus.CREATED)
                .build();

        orderRepository.save(order);

        // 发布订单创建事件
        eventPublisher.publish(OrderCreatedEvent.of(order));

        // 延迟30分钟发送超时取消事件
        String taskId = delayedPublisher.publishDelayed(
            OrderTimeoutEvent.builder()
                .orderId(order.getOrderId())
                .build(),
            30 * 60 * 1000  // 30分钟
        );

        // 保存任务ID，用于取消
        order.setTimeoutTaskId(taskId);
        orderRepository.save(order);

        return order;
    }

    /**
     * 支付订单 - 取消延迟任务
     */
    public void payOrder(String orderId) {
        Order order = orderRepository.findById(orderId);

        // 取消延迟超时任务
        if (order.getTimeoutTaskId() != null) {
            delayedPublisher.cancelDelayed(order.getTimeoutTaskId());
        }

        // 处理支付...
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }
}
```

### 场景2: 定时提醒

```java
@Service
public class ReminderService {

    @Autowired
    private DelayedEventPublisher delayedPublisher;

    /**
     * 创建会议 - 提前15分钟提醒
     */
    public void createMeeting(Meeting meeting) {
        // 发送会议创建事件
        eventPublisher.publish(MeetingCreatedEvent.of(meeting));

        // 计算提醒时间（会议开始前15分钟）
        LocalDateTime reminderTime = meeting.getStartTime()
            .minusMinutes(15);

        delayedPublisher.publishDelayedAt(
            MeetingReminderEvent.builder()
                .meetingId(meeting.getId())
                .reminderType("START_SOON")
                .build(),
            reminderTime
        );
    }
}
```

### 场景3: 延迟重试

```java
@Service
public class PaymentRetryService {

    @Autowired
    private DelayedEventPublisher delayedPublisher;

    /**
     * 处理支付失败 - 延迟重试
     */
    public void handlePaymentFailure(Payment payment) {
        if (payment.getRetryCount() >= 3) {
            // 达到最大重试次数
            payment.setStatus(PaymentStatus.FAILED);
            return;
        }

        // 计算重试延迟（指数退避）
        long delayMillis = calculateBackoff(payment.getRetryCount());

        // 延迟重试
        delayedPublisher.publishDelayed(
            PaymentRetryEvent.builder()
                .paymentId(payment.getId())
                .retryCount(payment.getRetryCount() + 1)
                .build(),
            delayMillis
        );
    }

    private long calculateBackoff(int retryCount) {
        // 1min, 2min, 4min, 8min...
        return Math.min(60_000L * (1L << retryCount), 10 * 60_000L);
    }
}
```

### 场景4: 批量延迟处理

```java
@Service
public class BatchProcessingService {

    @Autowired
    private DelayedEventPublisher delayedPublisher;

    /**
     * 批量延迟处理 - 分散任务避免峰值
     */
    public void scheduleBatchProcessing(List<Order> orders) {
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);

            // 每个订单延迟不同的时间（分散处理）
            long delayMillis = i * 100L; // 每个间隔100ms

            delayedPublisher.publishDelayed(
                OrderProcessEvent.builder()
                    .orderId(order.getOrderId())
                    .build(),
                delayMillis
            );
        }
    }
}
```

---

## 最佳实践

### 1. 任务ID管理

```java
// ✅ 推荐：保存任务ID以便取消
Order order = new Order();
order.setCancelTaskId(
    delayedPublisher.publishDelayed(event, delay)
);

// ❌ 避免：不保存任务ID，无法取消
delayedPublisher.publishDelayed(event, delay);  // 无法取消
```

### 2. 异常处理

```java
@Component
public class DelayedEventHandler {

    @StreamEventListener(streamKey = "order:timeout")
    public void handleOrderTimeout(OrderTimeoutEvent event) {
        try {
            // 检查订单状态
            Order order = orderRepository.findById(event.getOrderId());
            if (order.isPaid()) {
                // 已支付，忽略超时事件
                return;
            }

            // 取消订单
            order.cancel();
            orderRepository.save(order);

        } catch (Exception e) {
            log.error("处理订单超时失败: orderId={}", event.getOrderId(), e);
            // 不抛出异常，避免重试
        }
    }
}
```

### 3. 延迟时间选择

```java
// ✅ 短延迟（< 60秒）- 使用时间轮（内存）
delayedPublisher.publishDelayed(event, 5000);   // 5秒
delayedPublisher.publishDelayed(event, 30000);  // 30秒

// ✅ 长延迟（≥ 60秒）- 使用SortedSet（Redis）
delayedPublisher.publishDelayed(event, 3600000); // 1小时
delayedPublisher.publishDelayedAt(event, tomorrow);

// ⚠️ 注意：应用重启后，时间轮任务会恢复，但可能有短暂延迟
```

### 4. 监控指标

```java
@Component
public class DelayedQueueMonitor {

    @Autowired
    private DelayedEventPublisher delayedPublisher;

    @Scheduled(fixedRate = 60000)
    public void monitor() {
        int pendingCount = delayedPublisher.getPendingTaskCount();

        // 记录待处理任务数
        meterRegistry.gauge("delayed.queue.pending", pendingCount);

        // 告警阈值
        if (pendingCount > 10000) {
            alertService.sendAlert("延迟队列积压: " + pendingCount);
        }
    }
}
```

### 5. 容量规划

| 延迟时长 | 策略 | 容量 | 适用场景 |
|---------|------|------|---------|
| < 10秒 | 时间轮 | 100槽 | 高频短任务 |
| 10-60秒 | 时间轮 | 500槽 | 普通短任务 |
| 1-60分钟 | SortedSet | 无限制 | 中等延迟 |
| > 1小时 | SortedSet | 无限制 | 长期任务 |

---

## 性能指标

### 延迟精度

| 延迟时长 | 预期误差 | 说明 |
|---------|---------|------|
| < 10秒 | ±100ms | 时间轮精度 |
| 10-60秒 | ±200ms | 时间轮+扫描延迟 |
| 1-60分钟 | ±1秒 | SortedSet扫描间隔 |

### 吞吐量

| 场景 | QPS | 说明 |
|------|-----|------|
| 时间轮调度 | > 20万 | O(1)插入 |
| SortedSet调度 | > 5万 | ZADD操作 |
| 任务分发 | > 10万 | Pipeline批量 |

### 资源占用

| 资源 | 占用量 | 说明 |
|------|--------|------|
| 内存 | < 200MB | 时间轮+任务对象 |
| CPU | < 5% | 扫描线程 |
| Redis | < 100MB | SortedSet+备份 |

---

## 故障处理

### 1. 任务丢失

**现象**: 应用重启后，部分延迟任务丢失

**原因**: 时间轮任务在应用重启前未及时备份到Redis

**解决**:
```java
// 应用启动时从Redis恢复任务
@PostConstruct
public void recoverTasks() {
    timingWheelScheduler.recoverTasks();
}
```

### 2. 任务重复执行

**现象**: 延迟事件被处理多次

**原因**: 任务分发后标记失败，导致重复分发

**解决**:
```java
// 在消费端增加幂等性检查
if (idempotentChecker.isProcessed(eventId)) {
    return;
}
```

### 3. 延迟不准确

**现象**: 实际延迟与预期相差较大

**原因**: SortedSet扫描间隔过大或系统时间不一致

**解决**:
```yaml
# 减小扫描间隔
redis-toolkit:
  delayed:
    sorted-set:
      scan-interval: 500  # 500ms
```

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [延迟队列设计文档](../design/DELAY_QUEUE_DESIGN.md) | 架构设计和技术方案 |
| [业务接入指南](INTEGRATION_GUIDE.md) | 完整接入流程 |
| [调用链架构图](../design/CALL_CHAIN_ARCHITECTURE.md) | 调用链详解 |

---

**文档维护**: Redis ToolKit Team
**最后更新**: 2026-05-24
**版本**: v1.0.0
