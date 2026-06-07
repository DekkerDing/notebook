# Examples - Spring Boot RAG应用

> Notebook项目的核心应用模块，提供完整的RAG能力实现

## 📋 模块概述

`examples/` 是Notebook项目的主应用模块，基于Spring Boot 2.6.14 + Java 8构建，实现了完整的RAG（Retrieval-Augmented Generation）知识库向量能力底座。

### 核心功能

- **🧩 智能分块** - 支持6种文档分块策略
- **🔤 文本向量化** - 统一的向量化服务接口
- **🔍 多模式检索** - BM25、向量、混合检索
- **📚 知识库管理** - 完整的知识库CRUD和分类管理
- **🔌 ES向量存储** - 基于Elasticsearch 8.17的向量存储和检索

## 🏗️ 模块架构

### 分层架构

```
examples/
├── interfaces/              # 接入层
│   ├── knowledgebase/      # 知识库管理API
│   └── retrieval/          # 检索API
│
├── domain/                 # 领域层
│   ├── chunk/             # 分块领域
│   │   ├── model/        # 分块模型 (ChunkNode, ChunkConfig)
│   │   ├── strategy/     # 分块策略接口
│   │   │   ├── SeparatorChunkStrategy    # 分隔符分块
│   │   │   ├── RecursiveChunkStrategy    # 递归分块
│   │   │   ├── SemanticChunkStrategy     # 语义分块
│   │   │   └── TableChunkStrategy        # 表格分块
│   │   └── service/      # 分块服务
│   │
│   ├── embedding/        # 向量化领域
│   │   ├── model/        # 向量模型
│   │   ├── provider/     # 向量化提供商
│   │   └── service/      # 向量化服务
│   │
│   ├── retrieval/        # 检索领域
│   │   ├── model/        # 检索模型
│   │   ├── strategy/     # 检索策略
│   │   │   ├── BM25RetrieveStrategy      # BM25检索
│   │   │   └── VectorRetrieveStrategy    # 向量检索
│   │   └── service/      # 检索服务
│   │
│   └── knowledgebase/    # 知识库领域
│       ├── entity/       # 实体 (KbKnowledgeBase, KbKnowledgePoint)
│       ├── repository/   # JPA Repository
│       └── service/      # 知识库服务
│
├── infrastructure/        # 基础设施层
│   └── elasticsearch/    # ES适配器
│       ├── ElasticsearchConfig          # ES配置
│       ├── ElasticsearchVectorStore     # 向量存储
│       └── ElasticsearchVectorRetrieveService # 向量检索
│
└── common/               # 公共组件
    ├── ApiResponse       # 统一响应
    ├── PageResult        # 分页结果
    └── dto/             # 数据传输对象
```

## 🚀 快速开始

### 环境依赖

- **JDK**: 8+
- **Gradle**: 7.6.4
- **MySQL**: 8.0+ (可用H2替代)
- **Elasticsearch**: 8.17+ (向量检索必需)
- **Redis**: 6.0+ (可选，缓存)

### 构建运行

```bash
# 编译项目
./gradlew clean build

# 运行应用
./gradlew bootRun

# 指定Profile运行
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试
./gradlew test --tests "Elasticsearch*"

# 查看测试报告
open build/reports/tests/test/index.html
```

## ⚙️ 配置说明

### application.yml配置

```yaml
# Elasticsearch配置
elasticsearch:
  host: 192.168.10.107        # ES主机地址
  port: 9200                    # ES端口
  scheme: http                  # 协议
  vector-index-name: kb_vector_index  # 向量索引名
  vector-dimension: 1536        # 向量维度
  similarity: cosine            # 相似度算法
  connect-timeout: 5000         # 连接超时(ms)
  socket-timeout: 30000         # Socket超时(ms)

# Spring数据源配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/notebook
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

  # JPA配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    database-platform: org.hibernate.dialect.MySQL8Dialect

  # Redis配置
  redis:
    host: localhost
    port: 6379
    database: 0
```

## 📊 数据库设计

### 核心表结构

项目使用Flyway进行数据库版本管理，迁移脚本位于 `src/main/resources/db/migration/`：

#### 核心表（10个）

1. **kb_knowledge_base** - 知识库表
2. **kb_knowledge_point** - 知识点表
3. **kb_faq_pair** - FAQ问答对表
4. **kb_conversation_log** - 对话日志表
5. **kb_document** - 文档表
6. **kb_chunk** - 文档分块表
7. **kb_vector_index** - 向量索引表
8. **kb_agent_record** - Agent记录表
9. **kb_ticket** - 工单表
10. **kb_cache_event** - 缓存事件表

### 初始化数据

数据库包含以下初始化数据：
- 默认知识库（"通用知识库"）
- 示例FAQ对
- 示例知识点

## 🔌 API接口

### 知识库管理API

```
POST   /api/v1/knowledge-bases              # 创建知识库
GET    /api/v1/knowledge-bases              # 列表知识库
GET    /api/v1/knowledge-bases/{id}         # 获取知识库详情
PUT    /api/v1/knowledge-bases/{id}         # 更新知识库
DELETE /api/v1/knowledge-bases/{id}         # 删除知识库
PATCH  /api/v1/knowledge-bases/{id}/archive # 归档知识库
GET    /api/v1/knowledge-bases/{id}/stats   # 获取统计信息
```

### 检索API

```
POST   /api/v1/retrieval/search             # 搜索
POST   /api/v1/retrieval/similarity         # 相似度查询
POST   /api/v1/retrieval/multi-way          # 多路检索
POST   /api/v1/retrieval/batch              # 批量检索
POST   /api/v1/retrieval/vector             # 向量检索
```

### 统一响应格式

所有API返回统一的响应格式：

```json
{
  "code": "200",
  "message": "Success",
  "data": {},
  "timestamp": 1234567890,
  "traceId": "trace-123"
}
```

## 🧪 测试覆盖

### 测试基础设施

项目建立了完整的测试体系：

1. **TestProgressTracker** - 测试进度追踪
2. **RagAssertions** - RAG特定断言框架
3. **TestReportGenerator** - 测试报告生成器

### 测试套件

| 测试类 | 描述 | 状态 |
|--------|------|------|
| ElasticsearchConnectionTest | ES连接测试 | ✅ 100% |
| ElasticsearchVectorIntegrationTest | ES向量集成测试 | ✅ 100% |

### 测试文档

详见 `docs/` 目录：
- [RAG_Test_Execution_Log_2026-06-07.md](docs/RAG_Test_Execution_Log_2026-06-07.md)
- [RAG_Test_Status_Dashboard.md](docs/RAG_Test_Status_Dashboard.md)
- [RAG_Elasticsearch_Testing_Guide.md](docs/RAG_Elasticsearch_Testing_Guide.md)

## 🔧 开发指南

### 添加新的分块策略

1. 实现 `ChunkStrategy` 接口
2. 在 `ChunkSplitService` 中注册新策略
3. 添加单元测试

### 添加新的检索策略

1. 实现 `RetrieveStrategy` 接口
2. 在 `RetrieveService` 中注册新策略
3. 添加集成测试

### 添加新的向量化提供商

1. 实现 `EmbeddingProvider` 接口
2. 在 `EmbeddingService` 中注册提供商
3. 添加测试验证

## 📝 依赖说明

### 核心依赖

```gradle
// Spring Boot核心
implementation 'org.springframework.boot:spring-boot-starter'
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.boot:spring-boot-starter-validation'

// Jackson
implementation 'com.fasterxml.jackson.core:jackson-databind:2.13.5'
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.5'

// 数据库
runtimeOnly 'mysql:mysql-connector-java:8.0.30'
runtimeOnly 'com.h2database:h2:2.1.214'

// 测试
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

## 🚨 已知问题

1. [ ] 向量化服务使用Mock实现，需要接入真实API
2. [ ] Controller层部分接口待完善
3. [ ] 缺少OpenAPI/Swagger文档
4. [ ] 缺少完整的集成测试覆盖

## 📈 性能指标

基于当前测试结果的性能基线：

| 操作 | 目标延迟 | 实际延迟 | 状态 |
|------|---------|---------|------|
| 单文档索引 | <1000ms | ✅ | 达标 |
| 批量索引(50) | <5000ms | 2175ms | 优秀 |
| KNN检索 | <200ms | 155ms | 优秀 |
| 混合检索 | <200ms | 61ms | 优秀 |

## 🔜 下一步计划

### 短期 (1-2周)
- [ ] 接入真实向量化API（OpenAI/通义千问）
- [ ] 完善Controller层接口实现
- [ ] 添加API文档（Swagger/OpenAPI）
- [ ] 增强错误处理测试

### 中期 (1-2月)
- [ ] 实现GraphRAG模块
- [ ] 实现QA问答模块
- [ ] 添加文档解析器（PDF、Word、Excel）
- [ ] 实现多租户支持

### 长期 (3-6月)
- [ ] 性能优化和压测
- [ ] 监控和告警
- [ ] 部署文档
- [ ] 生产环境验证

---

**模块版本**: 0.1.0-SNAPSHOT
**最后更新**: 2026-06-07
**维护者**: Notebook RAG Team
