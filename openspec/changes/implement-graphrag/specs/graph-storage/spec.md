# Spec: 图存储

## ADDED Requirements

### Requirement: 图数据存储
系统SHALL提供图数据的持久化存储能力。

#### Scenario: 存储实体节点
- **WHEN** 系统抽取一个实体
- **THEN** 系统将实体保存到图存储
- **AND** 实体包含id、type、name、properties字段

#### Scenario: 存储关系边
- **WHEN** 系统识别一个关系
- **THEN** 系统将关系保存到图存储
- **AND** 关系包含source、target、type、confidence字段

### Requirement: 图存储选择
系统SHALL支持多种图存储后端。

#### Scenario: 使用Neo4j存储
- **WHEN** 系统配置使用Neo4j
- **THEN** 系统通过Neo4j Java Driver存储图数据
- **AND** 支持Cypher查询

#### Scenario: 使用内存图存储
- **WHEN** 系统配置使用内存图
- **THEN** 系统使用JGraphT存储图数据
- **AND** 图数据仅存在于运行时

### Requirement: 图操作接口
系统SHALL提供统一的图操作接口。

#### Scenario: 添加节点
- **WHEN** 调用graphStore.addNode(node)
- **THEN** 系统将节点添加到图中
- **AND** 返回操作结果

#### Scenario: 添加边
- **WHEN** 调用graphStore.addEdge(edge)
- **THEN** 系统将边添加到图中
- **AND** 验证source和target节点存在

#### Scenario: 查询节点
- **WHEN** 调用graphStore.findNode(id)
- **THEN** 系统返回节点信息
- **AND** 若节点不存在返回null

### Requirement: 图事务支持
系统SHALL支持图操作的事务处理。

#### Scenario: 批量添加节点事务
- **WHEN** 在事务中添加多个节点
- **THEN** 系统原子性地保存所有节点
- **AND** 失败时回滚所有操作

#### Scenario: 事务回滚
- **WHEN** 图操作过程中发生错误
- **THEN** 系统回滚未提交的操作
- **AND** 恢复图到操作前状态

### Requirement: 图索引管理
系统SHALL为图查询创建合适的索引。

#### Scenario: 实体名称索引
- **WHEN** 系统需要按名称查找实体
- **THEN** 系统使用实体名称索引
- **AND** 查询时间<100ms

#### Scenario: 实体类型索引
- **WHEN** 系统需要按类型查找实体
- **THEN** 系统使用实体类型索引
- **AND** 返回该类型的所有实体

### Requirement: 图数据备份
系统SHALL支持图数据的备份和恢复。

#### Scenario: 导出图数据
- **WHEN** 管理员执行图备份
- **THEN** 系统导出所有节点和边到JSON格式
- **AND** 包含所有属性信息

#### Scenario: 导入图数据
- **WHEN** 管理员执行图恢复
- **THEN** 系统从JSON文件导入图数据
- **AND** 清空现有数据后导入
