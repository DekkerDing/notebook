# Spec: 关系抽取

## ADDED Requirements

### Requirement: 关系识别
系统SHALL从文本中识别实体间的关系。

#### Scenario: 识别层次关系
- **WHEN** 文本表达"A是B的一部分"
- **THEN** 系统识别为PART_OF关系
- **AND** 记录关系方向和置信度

#### Scenario: 识别雇佣关系
- **WHEN** 文本表达"A在B工作"
- **THEN** 系统识别为WORKS_FOR关系
- **AND** 提取职位属性

#### Scenario: 识别地理关系
- **WHEN** 文本表达"A位于B"
- **THEN** 系统识别为LOCATED_IN关系
- **AND** 记录地理位置

### Requirement: 关系类型定义
系统SHALL支持预定义和自定义关系类型。

#### Scenario: 预定义关系类型
- **WHEN** 系统初始化
- **THEN** 加载预定义的关系类型
- **AND** 包含PART_OF、WORKS_FOR、LOCATED_IN等

#### Scenario: 自定义关系类型
- **WHEN** 管理员定义新关系类型
- **THEN** 系统保存关系类型定义
- **AND** 支持配置抽取规则

### Requirement: 关系属性提取
系统SHALL提取关系的相关属性。

#### Scenario: 提取时间属性
- **WHEN** 关系有时间维度
- **THEN** 系统提取时间信息
- **AND** 存储为关系属性

#### Scenario: 提取数量属性
- **WHEN** 关系有数量维度
- **THEN** 系统提取数量信息
- **AND** 存储为关系属性

### Requirement: 关系方向处理
系统SHALL正确处理有向关系的方向。

#### Scenario: 识别双向关系
- **WHEN** 关系本质是双向的（夫妻、合作）
- **THEN** 系统创建双向边
- **AND** 标记关系为双向

#### Scenario: 识别单向关系
- **WHEN** 关系是单向的（属于、雇佣）
- **THEN** 系统创建单向边
- **AND** 记录方向

### Requirement: 关系置信度评估
系统SHALL评估识别的关系的可信度。

#### Scenario: 计算关系置信度
- **WHEN** 系统识别一个关系
- **THEN** 系统计算置信度
- **AND** 置信度基于规则匹配度和上下文

#### Scenario: 过滤低置信度关系
- **WHEN** 关系置信度<0.7
- **THEN** 系统过滤该关系
- **AND** 不保存到图中

### Requirement: 复杂关系处理
系统SHALL处理复杂的关系表达。

#### Scenario: 解析多重关系
- **WHEN** 一个句子包含多个关系
- **THEN** 系统识别所有关系
- **AND** 分别存储到图中

#### Scenario: 解析嵌套关系
- **WHEN** 关系表达嵌套（A的B的C）
- **THEN** 系统拆分为多个直接关系
- **AND** 保留层级信息
