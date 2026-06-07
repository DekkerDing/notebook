# Notebook RAG系统架构设计

> **版本**: 0.1.0
> **状态**: Draft
> **作者**: Notebook RAG Team
> **日期**: 2026-06-07

## 文档概述

本文档描述Notebook RAG知识库向量能力底座的整体架构设计，包括技术选型、模块划分、接口设计和数据模型。

## 1. 系统概述

### 1.1 项目定位

Notebook是一个学习型+生产级的RAG（Retrieval-Augmented Generation）知识库向量系统，提供完整的文档处理、向量化、检索和知识库管理能力。

### 1.2 核心能力

| 能力 | 描述 | 状态 |
|------|------|------|
| 文档分块 | 支持6种分块策略 | ✅ 已实现 |
| 文本向量化 | 多提供商支持 | ✅ 接口定义 |
| 智能检索 | BM25+向量+混合检索 | ✅ 已实现 |
| 知识库管理 | CRUD和分类管理 | ✅ 已实现 |
| ES向量存储 | 基于ES 8.17 | ✅ 已实现 |
| GraphRAG | 知识图谱增强 | 🔄 计划中 |
| QA问答 | 智能问答 | 🔄 计划中 |

### 1.3 技术栈

- **语言**: Java 8
- **框架**: Spring Boot 2.6.14
- **数据库**: MySQL 8.0 / H2 (测试)
- **向量数据库**: Elasticsearch 8.17
- **缓存**: Redis 6.0+
- **构建**: Gradle 7.6.4

## 2. 架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                     接入层 (Access Layer)                      │
│  ┌──────────────────────┬───────────────────────────────┐   │
│  │  KbKnowledgeBase     │  RetrieveController           │   │
│  │  Controller           │                               │   │
│  └──────────────────────┴───────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                   编排层 (Orchestration Layer)                  │
│  ┌──────────────────────┬───────────────────────────────┐   │
│  │  KbKnowledgeBase     │  RetrieveService              │   │
│  │  Service              │                               │   │
│  └──────────────────────┴───────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                   领域服务层 (Domain Layer)                     │
│  ┌─────────┬───────────┬─────────┬──────────────────────┐   │
│  │ Chunk   │ Embedding │ Retrieve│ KnowledgeBase         │   │
│  │ Module  │ Module    │ Module  │ Module                │   │
│  └─────────┴───────────┴─────────┴──────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│               基础设施适配层 (Infrastructure Layer)             │
│  ┌──────────────────────┬───────────────────────────────┐   │
│  │  Elasticsearch       │  Cache Adapter                │   │
│  │  Vector Store        │                               │   │
│  └──────────────────────┴───────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                    存储层 (Storage Layer)                      │
│  ┌──────────┬──────────────┬──────────────┬──────────────┐   │
│  │ MySQL    │ Elasticsearch│ Redis         │ File Storage │   │
│  └──────────┴──────────────┴──────────────┴──────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                  外部服务层 (External Layer)                    │
│  ┌──────────┬──────────────┬──────────────┬──────────────┐   │
│  │ OpenAI   │ 通义千问      │ Local Model  │ Other APIs   │   │
│  └──────────┴──────────────┴──────────────┴──────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

#### 2.2.1 分块模块 (Chunk)

**职责**: 将文档按语义分割成适合向量化的文本块

**核心组件**:
- `ChunkNode` - 分块节点模型
- `ChunkConfig` - 分块配置
- `ChunkStrategy` - 分块策略接口
- `ChunkSplitService` - 分块服务

**分块策略**:
1. **SeparatorChunkStrategy** - 分隔符分块
   - 按段落、句子分割
   - 支持自定义分隔符
   
2. **RecursiveChunkStrategy** - 递归字符分块
   - 递归尝试不同分隔符
   - 保持语义完整性
   
3. **SemanticChunkStrategy** - 语义分块
   - 基于句子相似度分割
   - 保持语义连贯性
   
4. **TableChunkStrategy** - 表格分块
   - 支持Markdown、HTML、CSV表格
   - 保留表头信息

#### 2.2.2 向量化模块 (Embedding)

**职责**: 将文本转换为向量表示

**核心组件**:
- `EmbeddingService` - 向量化服务接口
- `EmbeddingProvider` - 向量化提供商接口
- `EmbeddingRequest` - 向量化请求
- `EmbeddingResult` - 向量化结果

**提供商支持**:
- OpenAI Embedding API
- 通义千问Embedding API
- 本地向量化模型

#### 2.2.3 检索模块 (Retrieve)

**职责**: 从知识库中检索相关内容

**核心组件**:
- `RetrieveService` - 检索服务
- `RetrieveStrategy` - 检索策略接口
- `RetrieveRequest` - 检索请求
- `RetrieveResult` - 检索结果

**检索策略**:
1. **BM25RetrieveStrategy** - BM25稀疏检索
   - 基于词频-逆文档频率
   - 适合关键词匹配
   
2. **VectorRetrieveStrategy** - 向量密集检索
   - 基于向量相似度
   - 适合语义匹配
   
3. **HybridRetrieveStrategy** - 混合检索
   - 结合BM25和向量检索
   - 支持权重配置

#### 2.2.4 知识库模块 (KnowledgeBase)

**职责**: 管理知识库、知识点、FAQ等

**核心组件**:
- `KbKnowledgeBase` - 知识库实体
- `KbKnowledgePoint` - 知识点实体
- `KbKnowledgeBaseService` - 知识库服务
- `KbKnowledgeBaseRepository` - 知识库仓储

**功能**:
- 知识库CRUD
- 知识点管理
- 分类管理
- 统计监控

### 2.3 基础设施层

#### 2.3.1 Elasticsearch适配器

**组件**:
- `ElasticsearchConfig` - ES配置
- `ElasticsearchVectorStore` - 向量存储
- `ElasticsearchVectorRetrieveService` - 向量检索
- `ElasticsearchRestTemplateConfig` - RestTemplate配置

**功能**:
- KNN向量索引创建
- 文档向量索引
- KNN检索
- 混合检索

## 3. 数据模型

### 3.1 核心实体

#### 3.1.1 知识库 (KbKnowledgeBase)

```yaml
字段:
  id: String (PK)
  name: String (知识库名称)
  description: String (描述)
  type: String (类型: general/faq/qa)
  status: String (状态: active/archived/deleted)
  category: String (分类)
  tags: List<String> (标签)
  metadata: Map (元数据)
  hitCount: Long (命中次数)
  satisfaction: Double (满意度 1-5)
  created_by: String (创建者)
  created_at: LocalDateTime
  updated_at: LocalDateTime
```

#### 3.1.2 知识点 (KbKnowledgePoint)

```yaml
字段:
  id: String (PK)
  kb_id: String (知识库ID, FK)
  title: String (标题)
  content: String (内容)
  type: String (类型: concept/fact/definition)
  difficulty: String (难度: easy/medium/hard)
  category: String (分类)
  tags: List<String> (标签)
  keywords: List<String> (关键词)
  related_points: List<String> (关联知识点)
  source: String (来源)
  confidence: Double (置信度 0-1)
  created_by: String
  created_at: LocalDateTime
  updated_at: LocalDateTime
```

#### 3.1.3 文档分块 (ChunkNode)

```yaml
字段:
  id: String
  content: String (分块内容)
  metadata: ChunkMetadata
  position: int (位置)
  parent_id: String (父节点ID)
```

### 3.2 向量索引

**Elasticsearch索引映射**:

```json
{
  "properties": {
    "content": {
      "type": "text",
      "fields": {
        "keyword": {"type": "keyword"}
      }
    },
    "content_vector": {
      "type": "dense_vector",
      "dims": 1536,
      "index": true,
      "similarity": "cosine"
    },
    "doc_id": {"type": "keyword"},
    "kb_id": {"type": "keyword"},
    "metadata": {
      "type": "object",
      "dynamic": true
    },
    "chunk_type": {"type": "keyword"},
    "created_at": {"type": "date"}
  }
}
```

## 4. API设计

### 4.1 统一响应格式

```json
{
  "code": "200",
  "message": "Success",
  "data": {},
  "timestamp": 1234567890,
  "traceId": "trace-123"
}
```

### 4.2 知识库API

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/v1/knowledge-bases | 创建知识库 |
| GET | /api/v1/knowledge-bases | 列表知识库 |
| GET | /api/v1/knowledge-bases/{id} | 获取详情 |
| PUT | /api/v1/knowledge-bases/{id} | 更新知识库 |
| DELETE | /api/v1/knowledge-bases/{id} | 删除知识库 |
| PATCH | /api/v1/knowledge-bases/{id}/archive | 归档知识库 |
| GET | /api/v1/knowledge-bases/{id}/stats | 获取统计 |

### 4.3 检索API

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/v1/retrieval/search | 搜索 |
| POST | /api/v1/retrieval/similarity | 相似度查询 |
| POST | /api/v1/retrieval/multi-way | 多路检索 |
| POST | /api/v1/retrieval/batch | 批量检索 |
| POST | /api/v1/retrieval/vector | 向量检索 |

## 5. 非功能需求

### 5.1 性能要求

| 操作 | 目标延迟 | 吞吐量 |
|------|---------|--------|
| 文档索引 | <1s | >10 docs/s |
| 向量检索 | <200ms | >5 queries/s |
| 混合检索 | <200ms | >5 queries/s |

### 5.2 可扩展性

- 支持百万级文档存储
- 支持千级并发检索
- 支持多知识库隔离

### 5.3 可靠性

- 数据持久化保证
- 索引自动恢复
- 降级策略

### 5.4 安全性

- 输入验证
- 输出过滤
- 访问控制（计划中）

## 6. 部署架构

### 6.1 开发环境

```
┌─────────────────┐
│  Developer PC   │
│  - Spring Boot  │
│  - H2 Database  │
│  - Mock ES      │
└─────────────────┘
```

### 6.2 测试环境

```
┌──────────────────────────────────────┐
│         Test Server                  │
│  ┌──────────────────────────────┐    │
│  │ Spring Boot Application     │    │
│  └──────────────────────────────┘    │
│           │         │                │
│           ▼         ▼                │
│  ┌──────────────┐  ┌─────────────┐  │
│  │ MySQL 8.0    │  │ ES 8.17     │  │
│  └──────────────┘  └─────────────┘  │
└──────────────────────────────────────┘
```

### 6.3 生产环境（计划）

```
┌─────────────────────────────────────────────┐
│              Load Balancer                  │
└─────────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
┌───────────┐  ┌───────────┐  ┌───────────┐
│ Instance 1│  │ Instance 2│  │ Instance 3│
└───────────┘  └───────────┘  └───────────┘
      │              │              │
      └──────────────┼──────────────┘
                     ▼
        ┌────────────────────────┐
        │  Shared Storage        │
        │  - MySQL Cluster       │
        │  - ES Cluster          │
        │  - Redis Cluster       │
        └────────────────────────┘
```

## 7. 开发路线图

### Phase 1: 基础能力 ✅
- [x] 分块模块
- [x] 向量化接口
- [x] 检索模块
- [x] ES向量存储

### Phase 2: 完善功能 🔄
- [ ] 真实向量化API集成
- [ ] 完整的API实现
- [ ] 文档解析器
- [ ] 缓存优化

### Phase 3: 高级功能 📋
- [ ] GraphRAG模块
- [ ] QA问答模块
- [ ] 多租户支持
- [ ] API限流

### Phase 4: 生产就绪 📋
- [ ] 监控告警
- [ ] 压力测试
- [ ] 部署自动化
- [ ] 文档完善

## 8. 参考资料

### 技术文档
- [Spring Boot文档](https://spring.io/projects/spring-boot)
- [Elasticsearch 8.17文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [RAG论文](https://arxiv.org/abs/2005.11401)

### 项目文档
- [README.md](../../README.md)
- [examples/README.md](../examples/README.md)
- [CONTRIBUTING.md](../CONTRIBUTING.md)
- [CHANGELOG.md](../CHANGELOG.md)

---

**文档状态**: Draft
**最后更新**: 2026-06-07
**版本**: 0.1.0
