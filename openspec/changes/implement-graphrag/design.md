# Design: GraphRAG模块实现

## Context

### 当前状态

现有RAG系统仅支持向量检索，存在以下局限：
1. 无法处理跨文档关联查询
2. 无法进行多跳推理
3. 缺少实体级别的上下文理解
4. 难以回答"为什么"类的问题

### 约束条件

- Java 8（无虚拟线程，无最新特性）
- Spring Boot 2.6.14
- 内存限制：JVM堆<2GB
- 图数据库：可选部署（支持内存图作为备选）
- NLP工具：考虑Java兼容性

### 利益相关者

- 用户：需要更准确的检索结果
- 运维：关注图数据库的资源消耗
- 开发：需要易于调试和测试的图系统

## Goals / Non-Goals

**Goals:**
1. 实现实体抽取（人物、组织、地点等）
2. 实现关系抽取（part_of、works_for、located_in等）
3. 支持图数据库存储（Neo4j或内存图）
4. 实现图查询API
5. 集成GraphRAG到检索流程
6. 支持图谱+向量混合检索

**Non-Goals:**
- 不实现实时图更新（批量更新）
- 不支持图可视化（使用Neo4j Browser）
- 不实现图挖掘算法（PageRank等）
- 不实现本体推理
- 不支持多模态实体（图像实体）

## Decisions

### 决策1: 图数据库选择

**选择**：Neo4j + 内存图双模式

**理由**：
- Neo4j企业版功能强大，社区版满足基本需求
- 支持Cypher查询语言，表达能力强大
- 提供Java Driver，集成方便
- 内存图作为备选，降低部署门槛

**架构**：
```java
interface GraphStore {
    void addNode(Node node);
    void addEdge(Edge edge);
    List<Node> query(Query query);
}

class Neo4jGraphStore implements GraphStore { }
class InMemoryGraphStore implements GraphStore { }
```

**替代方案**：
- ❌ 纯内存图：不支持持久化，重启丢失数据
- ❌ 其他图DB（ArangoDB、TigerGraph）：学习成本高
- ✅ **Neo4j + 内存图**：生产+开发双模式

### 决策2: 实体抽取策略

**选择**：基于规则的抽取 + NLP工具

**理由**：
- 基于规则的方法可控、可解释
- NLP工具提升准确率
- 不依赖外部API（离线运行）

**技术方案**：
- 规则引擎：使用Drools或自研规则
- NLP工具：考虑spaCy（需Python桥接）或Java NLP库
- 实体类型：可配置（人物、组织、地点、时间、数量）

**实现**：
```java
class EntityExtractor {
    List<Entity> extract(String text) {
        // 1. 分句
        // 2. 识别候选实体（NLP）
        // 3. 应用规则过滤
        // 4. 实体消歧
        // 5. 返回实体列表
    }
}
```

### 决策3: 关系抽取策略

**选择**：模板匹配 + 依存句法分析

**理由**：
- 模板匹配覆盖常见关系模式
- 依存句法分析识别主谓宾关系
- 支持自定义关系模板

**关系类型**：
- 层次关系：`part_of`（部分-整体）
- 隶属关系：`works_for`（雇佣）
- 地理位置：`located_in`（位于）
- 时间关系：`created_at`（创建时间）
- 关联关系：`related_to`（相关）

### 决策4: GraphRAG检索策略

**选择**：社区摘要 + 关系路径检索

**理由**：
- 社区摘要：Leiden算法发现图社区
- 关系路径：寻找实体间的最短路径
- 多跳推理：支持2-3跳关系查询

**检索流程**：
1. 从问题中识别实体
2. 查找实体在图中的位置
3. 提取实体所在社区的摘要
4. 提取实体间的路径关系
5. 将图谱上下文和向量上下文融合

### 决策5: 图存储模型

**选择**：属性图模型

**理由**：
- 实体和关系都可有属性
- 支持多类型节点和边
- 灵活的Schema设计

**数据模型**：
```cypher
// 实体节点
(:Entity {id, type, name, properties})

// 关系边
-[:RELATION {type, source, confidence}]
```

### 决策6: 图谱与向量的融合

**选择**：晚期融合（Late Fusion）

**理由**：
- 图谱检索和向量检索独立进行
- 在结果层面融合打分
- 保留各自的优势

**融合策略**：
```java
float graphScore = calculateGraphScore(query, graphContext);
float vectorScore = calculateVectorScore(query, vectorContext);
float hybridScore = 0.4 * graphScore + 0.6 * vectorScore;
```

## Risks / Trade-offs

### 风险1: 实体抽取准确率
**风险**：NLP工具准确率可能不理想

**缓解措施**：
- 提供人工审核接口
- 支持实体标注反馈
- 持续优化规则和模型

### 风险2: 图数据库性能
**风险**：图查询可能随规模增长变慢

**缓解措施**：
- 建立索引（实体、关系）
- 限制查询深度（最多3跳）
- 实现查询超时机制

### 风险3: 图谱构建成本
**风险**：全量构建图谱耗时较长

**缓解措施**：
- 增量更新机制
- 优先级队列（重要文档优先）
- 异步构建，不阻塞主流程

### 权衡1: 准确率 vs 性能
**权衡**：更准确的NLP模型通常更慢

**决策**：
- 初期使用规则+轻量NLP，性能优先
- 后期可集成更重模型（如BERT）

### 权衡2: 图谱复杂度 vs 可解释性
**权衡**：复杂图谱提供更多关系，但难以解释

**决策**：
- 限制关系类型数量（10-15种）
- 提供图谱可视化（Neo4j Browser）
- 支持关系解释API

## Migration Plan

### 阶段1: 基础架构（2周）
1. 创建graph模块
2. 实现图接口和基本模型
3. 选择并配置图存储方案
4. 添加单元测试

### 阶段2: 实体/关系抽取（3周）
1. 实现EntityExtractor
2. 实现RelationExtractor
3. 添加自定义规则支持
4. 集成NLP工具
5. 添加评估和测试

### 阶段3: 图查询（2周）
1. 实现Cypher查询支持（如使用Neo4j）
2. 实现路径查询
3. 实现社区发现
4. 添加图查询API

### 阶段4: GraphRAG集成（2周）
1. 实现GraphRetrieveStrategy
2. 实现混合检索
3. 集成到RAG流程
4. 性能优化

### 阶段5: 生产部署（1周）
1. 部署Neo4j（可选）
2. 性能调优
3. 监控告警
4. 文档完善

### 回滚策略
- GraphRAG作为可选检索策略
- 可通过配置禁用
- 不影响现有向量检索

## Open Questions

1. **Q**: 是否支持用户自定义实体类型？
   **A**: v1.0不支持，作为后续增强

2. **Q**: 如何处理实体歧义（如"苹果"可能是公司或水果）？
   **A**: 使用上下文消歧，或人工审核

3. **Q**: 是否需要实现时序图（Temporal Graph）？
   **A**: v1.0不考虑，未来可扩展

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                   RAG检索流程                            │
├─────────────────────────────────────────────────────────┤
│                                                            │
│  用户查询 ──┬─> 向量检索 ──┬─> 向量上下文               │
│              │               │                             │
│              └─> GraphRAG ───┴─> 图谱上下文               │
│                                                            │
│  融合 ──> 最终结果                                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  GraphRAG详细流程                         │
├─────────────────────────────────────────────────────────┤
│                                                            │
│  查询 ──> 实体识别 ──> 图查询 ──> 社区摘要 ──> 上下文 │
│            │             │              │                   │
│            └─> 关系识别 ──> 路径查询 ──> 关系路径 ──> 上下文 │
│                                                            │
└─────────────────────────────────────────────────────────┘
```

## Performance Estimates

基于预估的图规模和查询复杂度：

| 操作 | 图规模 | 预估时间 |
|------|--------|---------|
| 实体抽取 | 1KB文本 | ~500ms |
| 关系抽取 | 1KB文本 | ~800ms |
| 单跳查询 | 10K节点 | ~50ms |
| 两跳查询 | 10K节点 | ~150ms |
| 社区摘要 | 10K节点 | ~300ms |

## Data Model

### 实体模型

```java
class Entity {
    String id;
    String type;        // PERSON, ORG, LOC, etc.
    String name;
    String description;
    Map<String, Object> properties;
    List<String> aliases;
}

class Relation {
    String id;
    String type;        // PART_OF, WORKS_FOR, etc.
    Entity source;
    Entity target;
    Double confidence;
    Map<String, Object> properties;
}
```

### Neo4j存储模式

```cypher
// 创建实体节点
CREATE (e:Entity {
    id: 'entity-123',
    type: 'PERSON',
    name: '张三',
    description: '软件工程师'
})

// 创建关系
CREATE (e1:Entity {id: 'entity-123'})-[:WORKS_FOR {
    confidence: 0.95
}]->(e2:Entity {id: 'entity-456'})

// 查询两跳关系
MATCH path = (e1:Entity {name: '张三'})-[:WORKS_FOR*1..2]-(related)
RETURN related
```

## API Design

### 图查询API

```java
// 查询实体关系
GET /api/v1/graph/entities/{id}/relations

// 多跳查询
POST /api/v1/graph/query
{
  "entity": "张三",
  "relationType": "WORKS_FOR",
  "maxHops": 2
}

// 社区摘要
GET /api/v1/graph/communities/{id}/summary
```

### GraphRAG检索API

```java
// 图谱检索
POST /api/v1/retrieval/graph
{
  "query": "张三参与的项目有哪些？",
  "topK": 5
}
```
