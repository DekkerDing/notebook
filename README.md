# Redis Toolkit - Redis工具集

> **版本**: v1.0.0  
> **更新日期**: 2026 年 5 月 23 日  
> **项目类型**: Java工具库/Spring Boot Starter  
> **详细规范目录**: `openspec/conventions/`

---

## 项目简介

Redis Toolkit 是一个基于 Redis 的 Java 工具集，提供生产级别的 Redis 功能封装，帮助开发者快速构建高性能、高可用的分布式应用。

### 核心特性

| 特性 | 说明 | 状态 |
|------|------|------|
| **Redis Stream** | Redis替代Kafka，支持发布订阅、消费者组、分区、幂等、重试、DLQ | ✅ 已实现 |
| **分布式锁** | 可重入锁、读写锁、红锁、联锁 | ⏳ 计划中 |
| **两级缓存** | 本地缓存+分布式缓存 | ⏳ 计划中 |
| **限流** | 滑动窗口、令牌桶、漏桶 | ⏳ 计划中 |
| **ID生成** | 雪花算法、序列号、唯一ID | ⏳ 计划中 |
| **延时队列** | 有序集合实现、定时轮询 | ⏳ 计划中 |
| **地理服务** | GEO命令封装 | ⏳ 计划中 |
| **基数统计** | HyperLogLog UV统计 | ⏳ 计划中 |
| **布隆过滤器** | 误判判断、去重 | ⏳ 计划中 |
| **Lua脚本** | 脚本加载、执行、缓存 | ⏳ 计划中 |

---

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>io.github.dekkerding</groupId>
    <artifactId>redis-toolkit-stream</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 启用配置

```yaml
redis-toolkit:
  stream:
    event:
      enabled: true
      key-prefix: "stream:"
      default-group: "default-group"
      partition:
        enabled: true
        count: 3
        strategy: hash
```

### 发布事件

```java
@StreamEvent(streamKey = "order:stream")
public class OrderCreatedEvent extends DomainEvent {
    private String orderId;
    private BigDecimal amount;
    // ...
}

@Service
public class OrderService {
    @Autowired
    private EventPublishingService publishingService;
    
    public void createOrder(Order order) {
        // 业务逻辑...
        publishingService.publish(new OrderCreatedEvent(...));
    }
}
```

### 监听事件

```java
@Component
public class OrderEventHandler {
    
    @StreamEventListener(streamKey = "order:stream")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单事件: {}", event.getOrderId());
        // 处理业务逻辑...
    }
}
```

---

## 项目结构

```
redis-toolkit/
├── redis-toolkit-common/           # 公共模块
│   ├── annotation/                  # 注解定义
│   ├── exception/                   # 异常定义
│   ├── model/                       # 数据模型
│   ├── serializer/                  # 序列化
│   └── utils/                       # 工具类
├── redis-toolkit-core/             # 核心功能
│   ├── stream/                      # Stream功能
│   ├── lock/                        # 锁功能
│   ├── cache/                       # 缓存功能
│   ├── rate/                        # 限流功能
│   ├── id/                          # ID生成
│   └── delay/                       # 延时队列
├── redis-toolkit-extension/        # 扩展功能
│   ├── monitor/                     # 监控
│   ├── performance/                 # 性能优化
│   └── trace/                       # 链路追踪
├── redis-toolkit-api/              # API模块
│   └── autoconfigure/               # 自动配置
├── examples/                        # 示例项目
└── openspec/                        # 规范文档
    └── conventions/                  # 开发规范
```

---

## 模块依赖关系

```
┌─────────────────────────────────────────┐
│         业务应用 (引入redis-toolkit)        │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         redis-toolkit-api                 │
│         (自动配置、注解、启动引导)           │
└─────────────────────────────────────────┘
                    ↓
┌───────────────────┬───────────────────────┐
│  redis-toolkit-    │   redis-toolkit-      │
│  extension         │   core               │
│  (监控、性能、追踪)   │   (核心功能实现)        │
└───────────────────┴───────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         redis-toolkit-common              │
│         (公共组件、工具类、异常)             │
└─────────────────────────────────────────┘
```

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **JDK** | 8+ | 兼容性优先 |
| **Spring Boot** | 2.6+ | 自动配置支持 |
| **Spring Data Redis** | 2.6+ | Redis操作基础 |
| **Lettuce** | 6.2+ | 异步/响应式客户端 |
| **Lombok** | 1.18.30+ | 精简代码 |
| **Caffeine** | 3.0+ | 本地缓存 |
| **Jackson** | 2.15+ | JSON序列化 |

---

## 研发规范

项目采用工具库架构规范，详细规范见 `openspec/conventions/`：

| 规范文件 | 内容 |
|----------|------|
| `01-architecture.md` | 工具库架构、模块划分、技术栈 |
| `02-coding-standards.md` | 编码规范、接口设计、实现类模板 |
| `04-exception-logging.md` | 异常处理、日志规范 |
| `05-api-docs.md` | API文档规范 |
| `06-config-env.md` | 配置管理、环境变量 |
| `07-infra-runtime.md` | 运行时规范、定时任务 |
| `08-observability.md` | 监控指标、健康检查 |
| `09-unit-test.md` | 单元测试、集成测试 |

---

## 性能基准

| 功能 | 目标 | 测试条件 |
|------|------|---------|
| Stream发布 | >10K TPS | 单机、1KB消息 |
| Stream消费 | >10K TPS | 单消费者 |
| 分布式锁 | <10ms P99 | 无竞争 |
| 缓存GET | <1ms P99 | 本地+二级 |
| 限流判断 | <1ms P99 | 滑动窗口 |

---

## 文档中心

### 📋 项目规范

| 规范 | 说明 |
|------|------|
| [架构规范](openspec/conventions/01-architecture.md) | 工具库架构、模块划分、技术栈 |
| [编码规范](openspec/conventions/02-coding-standards.md) | 编码规范、接口设计、实现类模板 |
| [配置管理](openspec/conventions/06-config-env.md) | 配置文件、环境变量管理 |
| [可观测性](openspec/conventions/08-observability.md) | 监控指标、健康检查 |
| [单元测试](openspec/conventions/09-unit-test.md) | 测试规范、覆盖要求 |
| [更多规范...](openspec/INDEX.md) | 查看完整规范列表 |

### 📚 项目文档

| 文档 | 说明 |
|------|------|
| [部署检查清单](openspec/docs/deployment/DEPLOYMENT_CHECKLIST.md) | 部署前检查项 |
| [部署指南](openspec/docs/deployment/DEPLOYMENT.md) | 完整部署流程 |
| [性能优化指南](openspec/docs/optimization/OPTIMIZATION_GUIDE.md) | 性能优化方案 |
| [任务记录](openspec/tasks/Task.md) | 开发任务和测试记录 |
| [文档索引](openspec/INDEX.md) | 查看所有文档 |

### 🧪 测试文档

| 文档 | 说明 |
|------|------|
| [测试执行指南](openspec/tests/TEST_EXECUTION_GUIDE.md) | 如何运行测试 |
| [测试计划](openspec/tests/TEST_PLAN.md) | 测试计划和范围 |
| [测试报告](openspec/tests/TEST_REPORT.md) | 测试结果报告 |

---

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交变更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 许可证

本项目采用 Apache License 2.0 许可证

---

**项目维护**: Redis Toolkit Team  
**最后更新**: 2026-05-23