# Spec: 图查询

## ADDED Requirements

### Requirement: 多跳关系查询
系统SHALL支持多跳关系路径查询。

#### Scenario: 单跳查询
- **WHEN** 用户查询实体的直接关系
- **THEN** 系统返回所有1跳关系的实体
- **AND** 按关系类型分组返回

#### Scenario: 两跳查询
- **WHEN** 用户查询实体的两跳关系
- **THEN** 系统返回所有2跳关系的实体
- **AND** 提供完整的关系路径

#### Scenario: 限制跳数
- **WHEN** 用户查询且未指定跳数限制
- **THEN** 系统默认限制最多3跳
- **AND** 防止性能问题

### Requirement: 路径查询
系统SHALL支持查找实体间的最短路径。

#### Scenario: 查找最短路径
- **WHEN** 用户查询两个实体间的路径
- **THEN** 系统使用最短路径算法
- **AND** 返回路径上的所有节点和边

#### Scenario: 无路径情况
- **WHEN** 两个实体间不存在路径
- **THEN** 系统返回空路径
- **AND** 标记为"无连接"

### Requirement: 社区发现
系统SHALL支持图社区发现算法。

#### Scenario: 发现实体社区
- **WHEN** 用户查询实体的所属社区
- **THEN** 系统使用社区发现算法（如Leiden）
- **AND** 返回同一社区的所有实体

#### Scenario: 生成社区摘要
- **WHEN** 系统发现实体社区
- **THEN** 系统生成该社区的摘要信息
- **AND** 摘要包含社区内所有实体的核心信息

### Requirement: 模式匹配查询
系统SHALL支持基于模式的图查询。

#### Scenario: 三角模式查询
- **WHEN** 用户查询"A-B-A"三角模式
- **THEN** 系统找到所有满足模式的实体三元组
- **AND** 返回匹配的实体组合

#### Scenario: 星型模式查询
- **WHEN** 用户查询中心实体及其所有关联
- **THEN** 系统返回该实体的所有邻居
- **AND** 按关系类型分类返回

### Requirement: 图查询性能
系统SHALL在合理时间内完成图查询。

#### Scenario: 小型图查询
- **WHEN** 图节点数<1000
- **THEN** 系统在100ms内完成查询

#### Scenario: 中型图查询
- **WHEN** 图节点数在1000-10000
- **THEN** 系统在500ms内完成查询

### Requirement: 图查询API
系统SHALL提供图查询的REST API。

#### Scenario: 调用关系查询API
- **WHEN** 客户端调用GET /api/v1/graph/entities/{id}/relations
- **THEN** 系统返回该实体的所有关系
- **AND** 关联实体按距离排序

#### Scenario: 调用多跳查询API
- **WHEN** 客户端调用POST /api/v1/graph/query
- **THEN** 系统执行指定的图查询
- **AND** 返回查询结果
