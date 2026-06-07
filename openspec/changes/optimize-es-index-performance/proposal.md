# Proposal: 优化Elasticsearch索引性能

## Why

当前Elasticsearch批量索引性能不满足生产要求。测试显示索引50个文档耗时**2.175秒**，远超目标**500ms**，导致用户导入文档时等待时间过长，影响用户体验。随着知识库规模增长，性能问题会更加突出。

## What Changes

- **优化ES索引配置**：调整refresh_interval、bulk size等参数
- **实现批量索引优化**：优化bulk API调用策略
- **添加索引性能监控**：实时追踪索引延迟和吞吐量
- **实现索引队列机制**：大文档异步索引
- **优化向量序列化**：减少向量数据传输开销

## Capabilities

### New Capabilities

- **es-index-optimization**: Elasticsearch索引优化能力
  - 动态调整索引参数
  - 批量索引性能优化
  - 索引性能监控

### Modified Capabilities

无现有能力的行为变更（纯实现优化）

## Impact

**影响范围**：
- `ElasticsearchVectorStore` - 向量存储服务
- `ElasticsearchVectorRetrieveService` - 向量检索服务
- 测试套件 - 需要更新性能基准

**性能目标**：
- 单文档索引：<100ms（当前已达标）
- 批量索引（50文档）：<500ms（当前2175ms）
- 索引吞吐量：>100 docs/s（当前~23 docs/s）

**兼容性**：
- 无API变更
- 向后兼容
- 不影响现有功能
