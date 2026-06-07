# 功能快速参考卡

## 📋 功能清单

### 🔤 文档处理（3个）
- **PDF解析** - PdfParser
- **Word解析** - WordParser  
- **Excel解析** - ExcelParser

### 🧩 文档分块（4个）
- **分隔符分块** - SeparatorChunkStrategy
- **递归分块** - RecursiveChunkStrategy
- **语义分块** - SemanticChunkStrategy
- **表格分块** - TableChunkStrategy

### 🔍 检索策略（5个）
- **BM25检索** - BM25RetrieveStrategy → 关键词查询
- **向量检索** - VectorRetrieveStrategy → 语义查询
- **GraphRAG检索** - GraphRAGRetrieveStrategy → 关系查询
- **多路融合** - MultiWayRetrieveStrategy → 综合查询 ⭐
- **混合协调** - HybridRetrieveCoordinator → 灵活控制

### 🧠 知识图谱（4个）
- **图存储** - GraphStore → Neo4j/内存图
- **实体抽取** - EntityExtractor → 自动识别实体
- **关系抽取** - RelationExtractor → 自动识别关系
- **图查询** - GraphQueryService → 多跳/路径/社区

### 💾 存储服务（3个）
- **ES向量存储** - ElasticsearchVectorStore
- **并发索引** - ConcurrentIndexService
- **结果融合** - ResultFusionService

## 🎯 快速选择指南

| 查询类型 | 推荐策略 | 示例 |
|---------|---------|------|
| 精确关键词 | BM25 | "API接口配置" |
| 自然语言 | 向量 | "如何优化性能" |
| 实体关系 | GraphRAG | "张三在哪里工作" |
| 不确定 | 多路融合 ⭐ | 任意查询 |

## 🔗 API端点

| 功能 | 方法 | 端点 |
|-----|------|------|
| 文档上传 | POST | /api/v1/documents/upload |
| 支持类型 | GET | /api/v1/documents/supported-types |
| 实体关系 | GET | /api/v1/graph/entities/{id}/relations |
| 图查询 | POST | /api/v1/graph/query |
| 社区摘要 | GET | /api/v1/graph/communities/{id}/summary |
| 图统计 | GET | /api/v1/graph/stats |

## ⚙️ 核心配置

```yaml
# Elasticsearch
elasticsearch.host: 192.168.10.107
elasticsearch.port: 9200
elasticsearch.vector-dimension: 1536

# 图存储
graph.type: MEMORY  # 或 NEO4J
graph.entity-extraction.enabled: true

# 向量化
embedding.provider: openai  # 或 qianwen/mock
```

---

📖 **详细指南**: 参见 `FEATURE_GUIDE.md`
