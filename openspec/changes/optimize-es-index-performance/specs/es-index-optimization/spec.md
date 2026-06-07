# Spec: Elasticsearch索引优化

## ADDED Requirements

### Requirement: 批量索引性能目标
系统在批量索引文档向量时，SHALL在500ms内完成50个文档的索引操作。

#### Scenario: 批量索引50个文档
- **WHEN** 客户端调用批量索引API，传入50个文档向量
- **THEN** 系统应在500ms内完成索引
- **AND** 返回成功状态和文档ID列表

#### Scenario: 批量索引性能监控
- **WHEN** 执行批量索引操作
- **THEN** 系统记录索引延迟指标
- **AND** 系统计算索引吞吐量（docs/s）

### Requirement: 索引刷新间隔优化
系统在批量索引场景下，SHALL将Elasticsearch索引刷新间隔调整为30秒。

#### Scenario: 索引刷新配置
- **WHEN** 系统初始化或重新配置索引
- **THEN** 系统设置refresh_interval为30s
- **AND** 系统确保索引在30秒后可被搜索

#### Scenario: 手动刷新索引
- **WHEN** 用户需要立即搜索刚索引的文档
- **THEN** 系统提供手动刷新API
- **AND** 调用后索引立即可搜索

### Requirement: 动态批量大小
系统SHALL根据文档大小动态调整批量大小，范围为500-2000个文档。

#### Scenario: 小文档批量索引
- **WHEN** 文档平均大小<5KB
- **THEN** 系统使用批量大小2000

#### Scenario: 大文档批量索引
- **WHEN** 文档平均大小>20KB
- **THEN** 系统使用批量大小500

#### Scenario: 中等文档批量索引
- **WHEN** 文档平均大小在5-20KB之间
- **THEN** 系统使用批量大小1000

### Requirement: 并发索引控制
系统SHALL使用线程池并发索引文档，并发数不超过4个线程。

#### Scenario: 并发索引配置
- **WHEN** 系统初始化索引服务
- **THEN** 系统创建固定大小为4的线程池

#### Scenario: 并发索引执行
- **WHEN** 提交批量索引任务
- **THEN** 系统使用线程池并发执行
- **AND** 每个线程处理独立的批量请求

#### Scenario: 索引队列满
- **WHEN** 索引任务队列达到容量上限（1000）
- **THEN** 系统拒绝新任务并返回错误
- **AND** 错误消息包含"索引队列已满"

### Requirement: 索引错误处理
系统在索引失败时，SHALL自动重试最多3次，并记录错误日志。

#### Scenario: 索引超时重试
- **WHEN** 单个批量索引请求超时
- **THEN** 系统自动重试该请求
- **AND** 最多重试3次

#### Scenario: 索引失败记录
- **WHEN** 索引请求3次重试后仍失败
- **THEN** 系统记录错误日志
- **AND** 日志包含失败的文档ID列表
- **AND** 系统返回部分成功状态

### Requirement: 向量序列化优化
系统SHALL使用二进制格式传输向量数据，减少网络传输开销。

#### Scenario: 向量序列化
- **WHEN** 准备索引向量数据
- **THEN** 系统将float[]数组序列化为二进制格式
- **AND** 系统使用base64编码传输二进制数据

#### Scenario: 向量反序列化
- **WHEN** Elasticsearch接收向量数据
- **THEN** ES正确解析二进制向量
- **AND** 向量可用于KNN检索

### Requirement: 索引性能监控
系统SHALL通过Micrometer收集和暴露索引性能指标。

#### Scenario: 索引延迟指标
- **WHEN** 执行索引操作
- **THEN** 系统记录索引延迟（ms）
- **AND** 指标包含批量/单文档标签

#### Scenario: 索引吞吐量指标
- **WHEN** 执行索引操作
- **THEN** 系统计算并记录索引吞吐量（docs/s）
- **AND** 吞吐量基于最近100个索引请求计算

#### Scenario: 索引错误率指标
- **WHEN** 发生索引错误
- **THEN** 系统增加错误计数器
- **AND** 系统计算错误率（错误数/总数）

### Requirement: 索引配置可调
系统SHALL支持通过配置文件调整索引优化参数。

#### Scenario: 配置refresh_interval
- **WHEN** 管理员设置es.index.refresh-interval配置
- **THEN** 系统使用配置的刷新间隔

#### Scenario: 配置批量大小范围
- **WHEN** 管理员设置es.index.bulk.min和es.index.bulk.max配置
- **THEN** 系统使用配置的批量大小范围

#### Scenario: 配置并发数
- **WHEN** 管理员设置es.index.thread-pool-size配置
- **THEN** 系统使用配置的线程池大小
