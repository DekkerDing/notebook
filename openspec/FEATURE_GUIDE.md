# Notebook RAG 功能清单与接入指南

本文档提供Notebook项目中所有功能的完整清单、接入方式、使用方法、含义说明以及选择指南。

## 目录

1. [核心功能概览](#核心功能概览)
2. [文档处理功能](#文档处理功能)
3. [向量化功能](#向量化功能)
4. [检索功能](#检索功能)
5. [知识图谱功能](#知识图谱功能)
6. [API接口](#api接口)
7. [功能选择指南](#功能选择指南)
8. [配置参数](#配置参数)
9. [最佳实践](#最佳实践)

---

## 核心功能概览

### 功能分类图

```
Notebook RAG系统
├── 文档处理层
│   ├── 文档解析（PDF/Word/Excel）
│   └── 文档分块（6种策略）
├── 向量化层
│   └── 文本向量化服务
├── 检索层
│   ├── 稀疏检索（BM25）
│   ├── 密集检索（向量检索）
│   ├── 图谱检索（GraphRAG）
│   └── 多路融合检索
├── 知识图谱层
│   ├── 图存储（Neo4j/内存）
│   ├── 实体抽取
│   ├── 关系抽取
│   └── 图查询
└── 存储层
    ├── Elasticsearch（向量存储）
    ├── MySQL（结构化数据）
    ├── Redis（缓存）
    └── Neo4j（图存储）
```

### 功能速查表

| 功能模块 | 功能名称 | 类名 | API端点 | 状态 |
|---------|---------|------|---------|------|
| **文档解析** | PDF解析 | PdfParser | POST /api/v1/documents/upload | ✅ |
| **文档解析** | Word解析 | WordParser | POST /api/v1/documents/upload | ✅ |
| **文档解析** | Excel解析 | ExcelParser | POST /api/v1/documents/upload | ✅ |
| **文档分块** | 分隔符分块 | SeparatorChunkStrategy | - | ✅ |
| **文档分块** | 递归分块 | RecursiveChunkStrategy | - | ✅ |
| **文档分块** | 语义分块 | SemanticChunkStrategy | - | ✅ |
| **文档分块** | 表格分块 | TableChunkStrategy | - | ✅ |
| **向量化** | 文本向量化 | EmbeddingService | - | ✅ |
| **检索** | BM25检索 | BM25RetrieveStrategy | - | ✅ |
| **检索** | 向量检索 | VectorRetrieveStrategy | - | ✅ |
| **检索** | GraphRAG检索 | GraphRAGRetrieveStrategy | - | ✅ |
| **检索** | 多路融合检索 | MultiWayRetrieveStrategy | - | ✅ |
| **检索** | 混合协调 | HybridRetrieveCoordinator | - | ✅ |
| **图谱** | 图查询 | GraphQueryService | GET /api/v1/graph/* | ✅ |
| **存储** | ES向量存储 | ElasticsearchVectorStore | - | ✅ |
| **存储** | 并发索引 | ConcurrentIndexService | - | ✅ |

---

## 文档处理功能

### 1. 文档解析功能

#### 功能说明
支持PDF、Word、Excel三种文档格式的解析，提取文本内容、表格数据和元数据。

#### 类信息
- **PDF解析**: `io.github.dekkerding.examples.domain.document.parser.impl.PdfParser`
- **Word解析**: `io.github.dekkerding.examples.domain.document.parser.impl.WordParser`
- **Excel解析**: `io.github.dekkerding.examples.domain.document.parser.impl.ExcelParser`
- **解析器工厂**: `io.github.dekkerding.examples.domain.document.parser.DocumentParserFactory`

#### 接入方式

**方式1: 通过API接入**
```bash
POST /api/v1/documents/upload
Content-Type: multipart/form-data

文件参数: file
支持格式: .pdf, .doc, .docx, .xls, .xlsx
大小限制: 50MB
```

**方式2: 通过代码接入**
```java
@Autowired
private DocumentParserFactory parserFactory;

// 自动识别文档类型
DocumentParser parser = parserFactory.getParserByExtension("pdf");
ParsedDocument result = parser.parse(file);

// 或通过MIME类型
DocumentParser parser = parserFactory.getParser("application/pdf");
```

#### 返回结果
```json
{
  "fileName": "example.pdf",
  "fileType": "pdf",
  "textContent": "完整的文本内容...",
  "pages": [
    {
      "pageNumber": 1,
      "content": "第1页内容"
    }
  ],
  "tables": [
    {
      "pageNumber": 1,
      "headers": ["列1", "列2"],
      "rows": [["值1", "值2"]]
    }
  ],
  "metadata": {
    "author": "作者",
    "title": "标题",
    "page_count": 10
  },
  "parsedAt": "2026-06-07T10:00:00"
}
```

#### 使用场景
- 知识库文档导入
- 文档内容提取
- 表格数据提取
- 文档预处理

### 2. 文档分块功能

#### 功能说明
将长文档按照特定策略分割成适合检索的小块（chunks）。

#### 分块策略对比

| 策略 | 类名 | 适用场景 | 特点 | 推荐度 |
|-----|------|---------|------|-------|
| **分隔符分块** | SeparatorChunkStrategy | 结构化文档 | 按固定分隔符分割 | ⭐⭐⭐ |
| **递归分块** | RecursiveChunkStrategy | 通用文档 | 递归尝试多种分隔符 | ⭐⭐⭐⭐ |
| **语义分块** | SemanticChunkStrategy | 语义连贯文档 | 按语义相似度分割 | ⭐⭐⭐⭐⭐ |
| **表格分块** | TableChunkStrategy | 表格数据 | 保留表格结构 | ⭐⭐⭐⭐ |

#### 接入方式
```java
@Autowired
private ChunkSplitService chunkSplitService;

// 使用默认策略
List<ChunkNode> chunks = chunkSplitService.split(documentContent, ChunkConfig.builder().build());

// 指定策略
List<ChunkNode> chunks = chunkSplitService.split(
    documentContent, 
    ChunkConfig.builder()
        .strategy(ChunkType.SEMANTIC)
        .maxChunkSize(500)
        .chunkOverlap(50)
        .build()
);
```

#### 配置参数
```java
ChunkConfig config = ChunkConfig.builder()
    .strategy(ChunkType.RECURSIVE)      // 分块策略
    .maxChunkSize(1000)                  // 最大块大小（字符）
    .chunkOverlap(100)                    // 块重叠大小
    .separators(Arrays.asList("\n\n", "\n", "。"))  // 分隔符
    .build();
```

#### 使用建议
- **结构化文档**（法律合同、技术文档）：使用分隔符分块
- **普通文档**（文章、报告）：使用递归分块
- **需要语义连贯**（小说、论文）：使用语义分块
- **表格数据**（数据表、财务报表）：使用表格分块

---

## 向量化功能

### 文本向量化服务

#### 功能说明
将文本转换为向量表示，用于语义相似度计算。

#### 类信息
- **服务接口**: `io.github.dekkerding.examples.domain.embedding.service.EmbeddingService`
- **提供者接口**: `io.github.dekkerding.examples.domain.embedding.provider.EmbeddingProvider`
- **模拟实现**: `io.github.dekkerding.examples.domain.embedding.provider.impl.MockEmbeddingProvider`

#### 接入方式
```java
@Autowired
private EmbeddingService embeddingService;

// 单个文本向量化
EmbeddingResult result = embeddingService.embed("要向量化的文本");

// 批量向量化
List<String> texts = Arrays.asList("文本1", "文本2", "文本3");
List<EmbeddingResult> results = embeddingService.embedBatch(texts);

// 获取向量数组
float[] vector = result.getVector();
int dimension = vector.length;  // 向量维度
```

#### 向量配置
```yaml
embedding:
  provider: openai              # 提供商: openai/qianwen/mock
  model: text-embedding-ada-002 # 模型名称
  dimension: 1536               # 向量维度
  api-key: your-api-key         # API密钥
```

#### 使用场景
- 文档向量化存储
- 查询向量化
- 语义相似度计算
- RAG检索前处理

---

## 检索功能

### 检索策略总览

Notebook RAG提供5种检索策略，可根据查询特点选择：

#### 1. BM25稀疏检索

**类名**: `io.github.dekkerding.examples.domain.retrieval.strategy.impl.BM25RetrieveStrategy`

**功能**: 基于词频和逆文档频率的概率检索模型

**特点**:
- 精确关键词匹配
- 适合专有名词、术语查询
- 计算效率高

**接入方式**:
```java
@Autowired
private BM25RetrieveStrategy bm25Strategy;

// 先索引文档
bm25Strategy.indexDocument("doc1", "Java是一种编程语言...");

// 执行检索
RetrieveRequest request = RetrieveRequest.builder()
    .topK(10)
    .build();

List<RetrieveResult> results = bm25Strategy.retrieve("Java编程", request, context);
```

**适用场景**:
- ✅ 关键词查询（"Spring Boot配置"）
- ✅ 专业术语查询（"API接口"、"CRUD操作"）
- ✅ 精确匹配需求（产品型号、代码片段）
- ❌ 语义理解需求（"如何优化性能"）
- ❌ 概念性查询

#### 2. 向量密集检索

**类名**: `io.github.dekkerding.examples.domain.retrieval.strategy.impl.VectorRetrieveStrategy`

**功能**: 基于向量相似度的语义检索

**特点**:
- 语义相似度匹配
- 适合自然语言查询
- 发现隐含关联

**接入方式**:
```java
@Autowired
private VectorRetrieveStrategy vectorStrategy;

RetrieveRequest request = RetrieveRequest.builder()
    .topK(10)
    .minScore(0.7)  // 相似度阈值
    .build();

List<RetrieveResult> results = vectorStrategy.retrieve("如何提升系统性能", request, context);
```

**适用场景**:
- ✅ 语义查询（"如何优化"）
- ✅ 概念性查询（"架构设计原则"）
- ✅ 自然语言问题
- ❌ 精确关键词（产品型号、专有名词）
- ❌ 代码片段查询

#### 3. GraphRAG图谱检索

**类名**: `io.github.dekkerding.examples.domain.retrieval.strategy.impl.GraphRAGRetrieveStrategy`

**功能**: 基于知识图谱的关系推理检索

**特点**:
- 利用实体关系
- 支持多跳推理
- 社区摘要增强

**接入方式**:
```java
@Autowired
private GraphRAGRetrieveStrategy graphRAGStrategy;

// 图谱会自动从查询中识别实体并查询关系图
List<RetrieveResult> results = graphRAGStrategy.retrieve(
    "张三在哪个公司工作", 
    request, 
    context
);
```

**适用场景**:
- ✅ 关系查询（"X和Y的关系"）
- ✅ 实体关联查询（"谁在哪里工作"）
- ✅ 复杂推理（"A的客户的供应商"）
- ❌ 通用文本检索
- ❌ 缺乏图谱数据的领域

#### 4. 多路融合检索 ⭐ 推荐

**类名**: `io.github.dekkerding.examples.domain.retrieval.strategy.impl.MultiWayRetrieveStrategy`

**功能**: 并行执行多个策略并智能融合结果

**特点**:
- 并行执行，低延迟
- RRF算法融合
- 自适应权重调整
- 综合多种检索优势

**接入方式**:
```java
@Autowired
private MultiWayRetrieveStrategy multiWayStrategy;

// 自动选择并融合所有策略
List<RetrieveResult> results = multiWayStrategy.retrieve(
    "Spring Boot事务管理最佳实践", 
    request, 
    context
);

// 结果包含融合分数和各策略排名信息
results.forEach(r -> {
    System.out.println("文档: " + r.getDocId());
    System.out.println("融合分数: " + r.getScore());
    System.out.println("策略排名: " + r.getMetadata().get("strategy_ranks"));
});
```

**适用场景**:
- ✅ 综合查询需求
- ✅ 不确定查询类型
- ✅ 需要最高召回率
- ✅ 生产环境推荐

#### 5. 混合检索协调器

**类名**: `io.github.dekkerding.examples.domain.retrieval.coordinator.HybridRetrieveCoordinator`

**功能**: 统一的检索协调和管理服务

**特点**:
- 策略动态选择
- 结果后处理
- 策略元查询

**接入方式**:
```java
@Autowired
private HybridRetrieveCoordinator coordinator;

// 使用指定策略
RetrieveRequest request = RetrieveRequest.builder()
    .strategy("multiway")  // 指定策略
    .topK(10)
    .minScore(0.5)
    .build();

List<RetrieveResult> results = coordinator.retrieve("查询文本", request);

// 获取可用策略
List<String> strategies = coordinator.getAvailableStrategies();
// 输出: ["bm25", "vector", "graphrag", "multiway"]

// 执行多策略并分别返回结果
Map<String, List<RetrieveResult>> multiResults = 
    coordinator.retrieveMultiStrategy(query, request, 
        Arrays.asList("bm25", "vector"));
```

---

## 知识图谱功能

### 1. 图存储功能

#### 功能说明
支持Neo4j图数据库和内存图两种存储方式。

#### 类信息
- **图存储接口**: `io.github.dekkerding.examples.domain.graph.store.GraphStore`
- **内存图实现**: `io.github.dekkerding.examples.infrastructure.graph.InMemoryGraphStore`
- **图存储工厂**: `io.github.dekkerding.examples.infrastructure.graph.GraphStoreFactory`
- **图配置**: `io.github.dekkerding.examples.infrastructure.graph.GraphProperties`

#### 接入方式
```java
@Autowired
private GraphStoreFactory graphStoreFactory;

// 获取图存储实例
GraphStore graphStore = graphStoreFactory.getGraphStore();

// 添加节点
Entity entity = Entity.builder()
    .id("person:john_doe")
    .type("PERSON")
    .name("John Doe")
    .kbId("kb1")
    .build();

graphStore.addNode(entity);

// 添加关系
Relation relation = Relation.builder()
    .id("works_for:john->company")
    .source("person:john_doe")
    .target("company:acme")
    .type("WORKS_FOR")
    .confidence(0.9)
    .kbId("kb1")
    .build();

graphStore.addEdge(relation);

// 查询节点
Entity found = graphStore.findNode("person:john_doe");
```

#### 配置选择
```yaml
graph:
  type: MEMORY                  # 存储类型: NEO4J 或 MEMORY
  neo4j:
    host: localhost
    port: 7687
    username: neo4j
    password: password
  entity-extraction:
    enabled: true
    confidence-threshold: 0.7
  relation-extraction:
    enabled: true
    confidence-threshold: 0.6
```

**选择建议**:
- **开发/测试**: 使用MEMORY（无需额外部署）
- **生产环境**: 使用Neo4j（支持大规模图谱）

### 2. 图查询功能

#### 功能说明
支持多跳关系查询、最短路径查询、社区发现等。

#### 类信息
- **图查询服务**: `io.github.dekkerding.examples.domain.graph.query.GraphQueryService`
- **图查询模型**: `io.github.dekkerding.examples.domain.graph.query.GraphQuery`

#### 接入方式
```java
@Autowired
private GraphQueryService graphQueryService;

// 查询实体关系
GraphQuery relationQuery = GraphQuery.builder()
    .type(GraphQuery.QueryType.RELATION_QUERY)
    .entityId("person:john")
    .hops(2)  // 2跳关系
    .build();

GraphQuery.GraphQueryResult result = graphQueryService.execute(relationQuery);
result.getRelations().forEach(r -> System.out.println(r.getType()));

// 查询社区
GraphQuery communityQuery = GraphQuery.builder()
    .type(GraphQuery.QueryType.COMMUNITY_QUERY)
    .entityId("person:john")
    .build();

GraphQuery.GraphQueryResult result = graphQueryService.execute(communityQuery);
System.out.println(result.getCommunitySummary());
```

#### API接口
```bash
# 获取实体关系
GET /api/v1/graph/entities/{id}/relations?hops=2

# 执行图查询
POST /api/v1/graph/query
Content-Type: application/json

{
  "type": "RELATION_QUERY",
  "entityId": "person:john",
  "hops": 2
}

# 获取社区摘要
GET /api/v1/graph/communities/{id}/summary

# 获取图谱统计
GET /api/v1/graph/stats
```

### 3. 实体/关系抽取

#### 功能说明
自动从文本中抽取实体和关系，构建知识图谱。

#### 类信息
- **实体抽取接口**: `io.github.dekkerding.examples.domain.graph.extractor.EntityExtractor`
- **关系抽取接口**: `io.github.dekkerding.examples.domain.graph.extractor.RelationExtractor`
- **规则实现**: `RuleBasedEntityExtractor`, `RuleBasedRelationExtractor`

#### 支持的实体类型
- PERSON（人物）
- ORG（组织）
- LOC（地点）
- DATE（日期）
- NUMBER（数字）

#### 支持的关系类型
- WORKS_FOR（工作于）
- PART_OF（属于）
- LOCATED_IN（位于）

#### 配置
```yaml
graph:
  entity-extraction:
    supported-types: PERSON,ORG,LOC,DATE,NUMBER
    confidence-threshold: 0.7
    use-nlp: false  # false=规则, true=NLP工具
```

---

## API接口

### REST API端点

#### 文档处理API

```bash
# 上传并解析文档
POST /api/v1/documents/upload
Content-Type: multipart/form-data

# 获取支持的文件类型
GET /api/v1/documents/supported-types
```

#### 图谱API

```bash
# 获取实体关系
GET /api/v1/graph/entities/{id}/relations?hops=1

# 执行图查询
POST /api/v1/graph/query

# 获取社区摘要
GET /api/v1/graph/communities/{id}/summary

# 获取实体信息
GET /api/v1/graph/entities/{id}

# 获取图谱统计
GET /api/v1/graph/stats
```

### API响应格式

统一使用 `ApiResponse<T>` 格式：

```json
{
  "code": "200",
  "message": "Success",
  "data": { ... },
  "timestamp": 1686144000000
}
```

---

## 功能选择指南

### 检索策略选择树

```
开始
  │
  ├─ 查询包含精确关键词？
  │   └─ 是 → BM25检索
  │
  ├─ 查询是自然语言问题？
  │   └─ 是 → 向量检索
  │
  ├─ 查询涉及实体关系？
  │   └─ 是 → GraphRAG检索
  │
  ├─ 不确定查询类型？
  │   └─ 是 → 多路融合检索 ⭐
  │
  └─ 需要灵活控制？
      └─ 是 → 混合协调器
```

### 典型场景推荐

#### 场景1: 产品手册检索
**查询示例**: "如何配置Spring Boot数据源"

**推荐策略**: BM25检索 或 多路融合检索

**理由**: 包含精确技术术语，BM25擅长关键词匹配

#### 场景2: 概念性查询
**查询示例**: "微服务架构的设计原则是什么"

**推荐策略**: 向量检索 或 多路融合检索

**理由**: 需要理解语义，向量检索更合适

#### 场景3: 关系查询
**查询示例**: "张三参与了哪些项目"

**推荐策略**: GraphRAG检索

**理由**: 涉及实体关系，图谱推理更准确

#### 场景4: 综合查询
**查询示例**: "Spring Boot事务管理最佳实践"

**推荐策略**: 多路融合检索 ⭐

**理由**: 包含关键词和概念，融合多种策略效果最好

### 性能对比

| 策略 | 平均延迟 | 召回率 | 准确率 | 适用场景 |
|-----|---------|-------|-------|---------|
| BM25 | ~50ms | 中 | 高 | 关键词查询 |
| 向量 | ~150ms | 高 | 中 | 语义查询 |
| GraphRAG | ~200ms | 低 | 高 | 关系查询 |
| 多路融合 | ~300ms | 高 | 高 | 综合查询 |

---

## 配置参数

### Elasticsearch配置

```yaml
elasticsearch:
  host: 192.168.10.107
  port: 9200
  scheme: http
  vector-index-name: kb_vector_index
  vector-dimension: 1536
  similarity: cosine              # 相似度算法: cosine/dot_product/l2_norm
  
  # HNSW参数
  hnsw:
    m: 16                        # 每个节点的最大连接数
    ef-construction: 100          # 构建时的候选数
    ef-search: 50                 # 搜索时的候选数
  
  # 索引优化
  index-optimization:
    refresh-interval: 30          # 索引刷新间隔（秒）
    bulk-min-size: 500            # 最小批量大小
    bulk-max-size: 2000           # 最大批量大小
    thread-pool-size: 4           # 索引线程池大小
```

### 图存储配置

```yaml
graph:
  type: MEMORY                   # NEO4J 或 MEMORY
  
  # Neo4j配置
  neo4j:
    host: localhost
    port: 7687
    scheme: "bolt://"
    username: neo4j
    password: password
    database: neo4j
  
  # 实体抽取配置
  entity-extraction:
    enabled: true
    confidence-threshold: 0.7
    max-entities: 1000
    supported-types: PERSON,ORG,LOC,DATE,NUMBER
  
  # 关系抽取配置
  relation-extraction:
    enabled: true
    confidence-threshold: 0.6
    max-relations: 2000
    max-hops: 3
```

---

## 最佳实践

### 1. 文档处理最佳实践

```java
// 1. 上传文档
DocumentParser parser = parserFactory.getParserByExtension("pdf");
ParsedDocument doc = parser.parse(file);

// 2. 分块处理
ChunkConfig chunkConfig = ChunkConfig.builder()
    .strategy(ChunkType.SEMANTIC)
    .maxChunkSize(500)
    .chunkOverlap(50)
    .build();

List<ChunkNode> chunks = chunkSplitService.split(doc.getTextContent(), chunkConfig);

// 3. 向量化
List<float[]> vectors = new ArrayList<>();
for (ChunkNode chunk : chunks) {
    float[] vector = embeddingService.embed(chunk.getContent()).getVector();
    vectors.add(vector);
}

// 4. 索引存储
ConcurrentIndexService indexService = ...;
indexService.submitIndexTask(documents);
```

### 2. 检索最佳实践

```java
// 使用多路融合检索获得最佳结果
RetrieveRequest request = RetrieveRequest.builder()
    .strategy("multiway")          // 使用多路融合
    .topK(10)                     // 返回前10个结果
    .minScore(0.5)                 // 最小分数阈值
    .build();

List<RetrieveResult> results = coordinator.retrieve(query, request);

// 检查融合元数据
results.forEach(r -> {
    Map<String, Object> metadata = r.getMetadata();
    System.out.println("融合分数: " + metadata.get("fusion_score"));
    System.out.println("策略排名: " + metadata.get("strategy_ranks"));
});
```

### 3. 性能优化建议

1. **使用批量操作**
   - 批量向量化
   - 批量索引
   - 批量查询

2. **启用缓存**
   - 向量缓存
   - 查询结果缓存
   - 图谱查询缓存

3. **调整参数**
   - HNSW参数：ef-search增大可提高准确率但降低性能
   - 批量大小：根据文档大小动态调整
   - TopK：合理设置，避免返回过多结果

### 4. 错误处理

```java
try {
    List<RetrieveResult> results = coordinator.retrieve(query, request);
    
    if (results.isEmpty()) {
        // 回退到向量检索
        request.setStrategy("vector");
        results = coordinator.retrieve(query, request);
    }
    
} catch (Exception e) {
    log.error("检索失败", e);
    // 返回空结果或使用备用策略
}
```

---

## 常见问题

### Q1: 如何选择合适的检索策略？

**A**: 参考本文档的"功能选择指南"部分，或使用多路融合检索自动选择。

### Q2: 如何提升检索准确率？

**A**: 
1. 使用多路融合检索
2. 调整各策略权重
3. 提高文档质量
4. 优化分块策略

### Q3: 如何处理大量文档？

**A**: 
1. 使用并发索引服务
2. 启用动态批量大小
3. 使用索引优化配置
4. 考虑使用Elasticsearch集群

### Q4: 图谱检索什么时候有用？

**A**: 
- 查询涉及实体关系
- 需要多跳推理
- 领域知识关系复杂
- 已有完善的图谱数据

---

**文档版本**: 1.0.0
**更新日期**: 2026-06-07
**维护者**: Notebook RAG Team
