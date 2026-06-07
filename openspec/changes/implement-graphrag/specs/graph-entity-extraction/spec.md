# Spec: 实体抽取

## ADDED Requirements

### Requirement: 实体识别
系统SHALL从文本中识别并抽取实体。

#### Scenario: 识别人物实体
- **WHEN** 文本包含人物名称
- **THEN** 系统识别为PERSON类型实体
- **AND** 提取实体名称和位置

#### Scenario: 识别组织实体
- **WHEN** 文本包含组织名称
- **THEN** 系统识别为ORG类型实体
- **AND** 记录组织全称和简称

#### Scenario: 识别地点实体
- **WHEN** 文本包含地理位置名称
- **THEN** 系统识别为LOC类型实体
- **AND** 提取地点层级（国家/城市/地区）

### Requirement: 实体消歧
系统SHALL对识别出的实体进行消歧处理。

#### Scenario: 消歧同名实体
- **WHEN** 识别到"苹果"这个实体
- **THEN** 系统根据上下文判断是公司还是水果
- **AND** 添加消歧置信度

#### Scenario: 实体链接
- **WHEN** 识别到"张三"和"张三丰"指向同一人
- **THEN** 系统将其合并为同一实体
- **AND** 保留别名信息

### Requirement: 实体属性提取
系统SHALL从文本中提取实体的属性信息。

#### Scenario: 提取人物属性
- **WHEN** 识别到人物实体
- **THEN** 系统提取相关属性（职位、邮箱等）
- **AND** 属性存储在properties中

#### Scenario: 提取组织属性
- **WHEN** 识别到组织实体
- **THEN** 系统提取相关属性（行业、规模等）
- **AND** 属性存储在properties中

### Requirement: 实体类型自定义
系统SHALL支持用户自定义实体类型。

#### Scenario: 定义实体类型
- **WHEN** 管理员定义新的实体类型
- **THEN** 系统创建实体类型定义
- **AND** 支持配置识别规则

#### Scenario: 使用自定义类型
- **WHEN** 用户上传文档时
- **THEN** 系统使用配置的实体类型进行识别

### Requirement: 实体抽取性能
系统SHALL在合理时间内完成实体抽取。

#### Scenario: 小文档实体抽取
- **WHEN** 文档<10KB
- **THEN** 系统在500ms内完成抽取

#### Scenario: 大文档实体抽取
- **WHEN** 文档>=10KB
- **THEN** 系统使用异步抽取
- **AND** 返回任务ID供查询

### Requirement: 实体质量评估
系统SHALL对抽取的实体进行质量评估。

#### Scenario: 评估实体置信度
- **WHEN** 系统识别一个实体
- **THEN** 系统计算识别置信度
- **AND** 置信度范围0-1

#### Scenario: 过滤低质量实体
- **WHEN** 实体置信度<0.6
- **THEN** 系统过滤该实体
- **AND** 不保存到图中
