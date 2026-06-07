# Design: Elasticsearch索引性能优化

## Context

### 当前状态

现有Elasticsearch向量存储服务(`ElasticsearchVectorStore`)采用同步批量索引策略，主要问题：

1. **刷新机制频繁**：ES默认1秒刷新，每次索引都触发
2. **批量大小固定**：当前使用固定批量大小，未根据数据特征优化
3. **向量序列化开销**：1536维向量序列化和传输耗时较大
4. **缺少并发控制**：单线程顺序索引，未利用并发能力

### 约束条件

- Java 8（无虚拟线程支持）
- ES 8.17单节点（192.168.10.107）
- Spring Boot 2.6.14
- 向量维度：1536（OpenAI embedding）
- 现有API不能变更

### 利益相关者

- 用户：希望快速导入文档
- 运维：关注ES资源使用
- 开发：需要易于维护的代码

## Goals / Non-Goals

**Goals:**
1. 批量索引50文档 <500ms（当前2175ms）
2. 索引吞吐量 >100 docs/s（当前~23 docs/s）
3. 实时性能监控
4. 向后兼容，无API变更

**Non-Goals:**
- 不改变向量维度
- 不修改索引结构
- 不引入新的依赖服务（如RabbitMQ）
- 不实现分布式索引（单ES节点）

## Decisions

### 决策1: 调整ES refresh_interval

**选择**：将refresh_interval从默认1s调整为30s

**理由**：
- ES每次刷新开销大（段合并、写入磁盘）
- 批量索引场景不需要实时可见
- 30s后索引仍可搜索，满足用户预期

**替代方案**：
- ❌ 禁用刷新（`-1`）：用户等待30s才能搜索
- ❌ 保持1s：频繁刷新影响性能
- ✅ **30s**：平衡性能和可用性

### 决策2: 优化批量大小策略

**选择**：动态批量大小（500-2000）

**理由**：
- 当前批量大小50太小，网络开销占比高
- ES bulk API建议批量大小5-15MB
- 1536维float向量约6KB，500个向量约3MB

**替代方案**：
- ❌ 固定大批量（5000）：内存压力大，失败回滚成本高
- ❌ 固定小批量（100）：网络开销大
- ✅ **动态批量**：根据文档大小自适应

### 决策3: 向量序列化优化

**选择**：使用二进制格式传输向量

**理由**：
- 当前JSON序列化1536维float约12KB
- 二进制格式可减少50%传输大小
- Jackson支持byte[]处理

**实现**：
```java
// 当前
"vector": [0.1, 0.2, ...]  // JSON

// 优化后  
"vector": "base64编码的二进制"  // 更紧凑
```

### 决策4: 并发索引策略

**选择**：使用ExecutorService线程池

**理由**：
- Java 8可用线程池
- 控制并发数避免ES过载
- 失败重试机制

**配置**：
```java
int poolSize = Math.min(4, Runtime.getRuntime().availableProcessors());
int queueSize = 1000;
```

### 决策5: 性能监控

**选择**：集成Micrometer指标收集

**理由**：
- Spring Boot Actuator已集成
- 支持Prometheus导出
- 实时监控索引延迟、吞吐量

**指标**：
- `es_index_latency`: 索引延迟
- `es_index_throughput`: 索引吞吐量
- `es_index_errors`: 索引错误率

## Risks / Trade-offs

### 风险1: ES内存溢出
**风险**：大批量索引可能耗尽ES堆内存

**缓解措施**：
- 限制最大批量大小（2000文档）
- 监控ES JVM堆使用率
- 实现背压机制

### 风险2: 网络超时
**风险**：大bulk请求可能超时

**缓解措施**：
- 增加socket超时到60s
- 实现请求分片
- 添加重试机制（最多3次）

### 权衡1: 刷新延迟 vs 索引可见性
**权衡**：30s refresh_interval意味着索引后30s才可搜索

**决策**：可接受
- 批量导入场景用户通常先导入再搜索
- 提供手动刷新API供急需搜索时使用

### 权衡2: 并发数 vs ES稳定性
**权衡**：高并发可能影响ES查询性能

**决策**：
- 限制并发数（4线程）
- 优先保证查询性能
- 索引使用低优先级线程

## Migration Plan

### 阶段1: 配置优化（立即部署）
1. 更新ES索引配置：refresh_interval=30s
2. 更新RestTemplate超时配置
3. 部署并观察

### 阶段2: 代码优化（1周）
1. 实现动态批量大小
2. 优化向量序列化
3. 添加性能监控

### 阶段3: 并发优化（1周）
1. 实现线程池索引
2. 添加索引队列
3. 实现失败重试

### 回滚策略
- 所有改动向后兼容
- 可通过配置回退到原始行为
- 保留原有单线程索引方法

## Open Questions

1. **Q**: 是否需要实现索引优先级队列？
   **A**: 初期不实现，根据实际使用反馈决定

2. **Q**: 是否需要支持索引暂停/恢复？
   **A**: v1.0不实现，作为后续增强

3. **Q**: 大文档（>10MB）如何处理？
   **A**: 单独实现大文档索引流程，不在本次优化范围

## Performance Estimates

基于测试数据和优化方案预估：

| 场景 | 当前 | 优化后 | 提升 |
|------|------|--------|------|
| 50文档批量索引 | 2175ms | ~400ms | 5.4x |
| 索引吞吐量 | 23 docs/s | ~125 docs/s | 5.4x |
| 单文档索引 | ~100ms | ~80ms | 1.25x |

## Monitoring

### 新增指标

```java
@Component
public class IndexMetrics {
    private final MeterRegistry meterRegistry;

    public void recordIndex(long duration, int docCount) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("es.index.duration")
                .tag("batch", "true")
                .register(meterRegistry));

        Gauge.builder("es.index.throughput", this, 
                obj -> obj.calculateThroughput())
                .register(meterRegistry);
    }
}
```

### 告警规则

- 索引延迟P99 > 1s：告警
- 索引错误率 > 1%：告警
- ES堆使用率 > 80%：告警
