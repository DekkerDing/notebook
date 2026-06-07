# Spec: GraphRAG检索

## ADDED Requirements

### Requirement: 图谱上下文检索
系统SHALL基于知识图谱增强检索上下文。

#### Scenario: 识别查询实体
- **WHEN** 用户提交查询请求
- **THEN** 系统从查询中识别相关实体
- **AND** 在图中定位这些实体

#### Scenario: 提取实体社区
- **WHEN** 定位到相关实体
- **THEN** 系统提取实体所属社区
- **AND** 生成社区摘要作为上下文

#### Scenario: 提取关系路径
- **WHEN** 定位到相关实体
- **THEN** 系统提取实体间的关系路径
- **AND** 路径信息作为上下文

### Requirement: 混合检索
系统SHALL支持图谱检索与向量检索的混合。

#### Scenario: 执行混合检索
- **WHEN** 用户使用GraphRAG检索
- **THEN** 系统同时执行图谱检索和向量检索
- **AND** 在结果层面融合两种检索的上下文

#### Scenario: 调整融合权重
- **WHEN** 系统执行混合检索
- **THEN** 系统使用可配置的权重（默认图谱40%，向量60%）
- **AND** 权重可通过配置调整

### Requirement: GraphRAG检索策略
系统SHALL作为独立的检索策略集成到RAG系统。

#### Scenario: 注册GraphRAG策略
- **WHEN** 系统初始化检索服务
- **THEN** 系统注册GraphRAG检索策略
- **AND** 标识为"graphrag"

#### Scenario: 执行GraphRAG检索
- **WHEN** 用户选择GraphRAG检索方式
- **THEN** 系统使用图谱上下文进行检索
- **AND** 返回增强的检索结果

### Requirement: 检索结果排序
系统SHALL基于图谱信息优化检索结果排序。

#### Scenario: 基于关系排序
- **WHEN** 检索结果包含图谱关系
- **THEN** 系统提升有直接关系实体的排序
- **AND** 提升中心实体的排序

#### Scenario: 基于社区排序
- **WHEN** 检索结果来自同一社区
- **THEN** 系统提升这些实体的排序
- **AND** 体现社区相关性

### Requirement: 检索结果解释
系统SHALL提供检索结果的图谱解释。

#### Scenario: 生成检索解释
- **WHEN** 返回GraphRAG检索结果
- **THEN** 系统提供检索依据的图谱路径
- **AND** 标注关系类型和置信度

#### Scenario: 可视化检索路径
- **WHEN** 用户需要理解检索逻辑
- **THEN** 系统返回图谱路径的文本描述
- **AND** 描述包含实体和关系的名称

### Requirement: 检索性能
系统SHALL在合理时间内完成GraphRAG检索。

#### Scenario: 小型图谱检索
- **WHEN** 图节点数<1000
- **THEN** 系统在300ms内完成检索

#### Scenario: 大型图谱检索
- **WHEN** 图节点数在1000-10000
- **THEN** 系统在1s内完成检索

### Requirement: 检索回退机制
系统SHALL在图谱检索失败时回退到向量检索。

#### Scenario: 图谱检索失败
- **WHEN** 图谱查询失败或超时
- **THEN** 系统自动回退到纯向量检索
- **AND** 记录回退日志

#### Scenario: 无相关实体
- **WHEN** 查询在图中找不到相关实体
- **THEN** 系统使用纯向量检索
- **AND** 在响应中标记"无图谱上下文"
