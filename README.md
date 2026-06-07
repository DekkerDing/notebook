# Notebook - 知识库向量能力底座

> 一个学习型+生产级的RAG（Retrieval-Augmented Generation）知识库向量系统，提供完整的分块、向量化、检索、知识库管理能力。

## 📋 项目概述

Notebook是一个基于Spring Boot构建的知识库向量能力底座，旨在提供生产级的RAG系统解决方案。项目名称寓意"知识的笔记本"，支持知识的存储、检索和智能问答。

### 核心能力

- **🧩 文档分块** - 支持6种分块策略（分隔符、递归、语义、表格、层次标题、代码）
- **🔤 文本向量化** - 支持OpenAI/通义千问/本地模型
- **🔍 智能检索** - BM25稀疏检索、向量密集检索、多路融合检索、GraphRAG图谱检索
- **🔀 混合协调** - 检索策略智能编排，RRF算法结果融合，自适应权重调整
- **📚 知识库管理** - 完整的知识库CRUD、分类管理、统计监控
- **📄 文档解析** - 支持PDF、Word、Excel文档解析和内容提取
- **🧠 GraphRAG** - 知识图谱增强检索，支持实体/关系抽取和社区发现
- **⚡ 性能优化** - 动态批量索引、并发索引、向量二进制序列化
- **📖 设计决策** - 完整的技术决策记录和思考过程文档化
- **💬 QA问答** - 基于知识库的智能问答（规划中）

## 🏗️ 项目架构

### 六层架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    接入层 (Controller)                        │
├─────────────────────────────────────────────────────────────┤
│                    编排层 (Service/Orchestrator)              │
├─────────────────────────────────────────────────────────────┤
│                  领域服务层 (Domain Service)                  │
│  ┌─────────────┬──────────────┬──────────────┬─────────────┐│
│  │ Chunk模块    │ Embedding模块  │ Retrieve模块   │ KB模块      ││
│  └─────────────┴──────────────┴──────────────┴─────────────┘│
├─────────────────────────────────────────────────────────────┤
│               基础设施适配层 (Infrastructure Adapter)         │
│  ┌──────────────┬─────────────────┬──────────────────────┐ │
│  │ ES Adapter    │ Vector DB        │ Cache Adapter        │ │
│  └──────────────┴─────────────────┴──────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    存储层 (Storage)                          │
│  ┌──────────────┬─────────────────┬──────────────────────┐ │
│  │ MySQL        │ Elasticsearch    │ Redis                 │ │
│  └──────────────┴─────────────────┴──────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                   外部服务层 (External)                       │
│  ┌──────────────┬─────────────────┬──────────────────────┐ │
│  │ OpenAI API   │ 通义千问API      │ 本地Embedding模型     │ │
│  └──────────────┴─────────────────┴──────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈

- **语言**: Java 8
- **框架**: Spring Boot 2.6.14
- **数据库**: MySQL 8.0 / H2 (测试)
- **向量数据库**: Elasticsearch 8.17
- **缓存**: Redis
- **构建工具**: Gradle 7.6.4
- **测试框架**: JUnit 5 + Spring Boot Test

## 📁 项目结构

```
notebook/
├── examples/                    # 主应用模块 (Spring Boot)
│   ├── src/main/java/
│   │   └── io/github/dekkerding/examples/
│   │       ├── domain/         # 领域层
│   │       │   ├── chunk/      # 分块模块
│   │       │   ├── embedding/   # 向量化模块
│   │       │   ├── retrieval/   # 检索模块
│   │       │   ├── knowledgebase/# 知识库模块
│   │       │   ├── document/   # 文档解析模块
│   │       │   └── graph/      # 图谱模块
│   │       ├── infrastructure/# 基础设施层
│   │       │   └── elasticsearch/# ES适配器
│   │       │   └── graph/      # 图存储适配器
│   │       ├── interfaces/     # 接入层
│   │       │   ├── document/   # 文档API
│   │       │   └── graph/      # 图谱API
│   │       └── common/         # 公共组件
│   │   └── resources/
│   │       └── db/migration/   # 数据库迁移脚本
│   ├── src/test/java/          # 测试代码
│   └── docs/                   # 项目文档
├── openspec/                    # OpenSpec规范和文档管理
│   ├── config.yaml             # OpenSpec配置
│   ├── CHANGELOG.md            # 变更日志
│   ├── CONTRIBUTING.md         # 贡献指南
│   ├── DOC_MANAGEMENT_RULES.md # 文档管理规则
│   ├── specs/                  # 规范文档
│   │   └── system-architecture.md
│   └── changes/                # 变更记录
│       ├── optimize-es-index-performance/
│       ├── add-document-parser/
│       └── implement-graphrag/
├── notes/                       # 学习笔记
└── README.md                    # 项目说明
```

## 🚀 快速开始

### 环境要求

- JDK 8+
- Gradle 7.x
- MySQL 8.0+ (可选，可用H2)
- Elasticsearch 8.17+ (向量检索)
- Redis 6.0+ (可选，缓存)

### 构建运行

```bash
# 克隆项目
git clone <repository-url>
cd notebook

# 进入主模块
cd examples

# 构建项目
./gradlew clean build

# 运行应用
./gradlew bootRun

# 运行测试
./gradlew test
```

### 配置说明

编辑 `examples/src/main/resources/application.yml`:

```yaml
elasticsearch:
  host: 192.168.10.107
  port: 9200
  vector-index-name: kb_vector_index
  vector-dimension: 1536
  similarity: cosine

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/notebook
    username: root
    password: your-password
```

## 🧪 测试

项目已建立完整的测试体系：

### 测试基础设施
- **TestProgressTracker** - 测试进度追踪
- **RagAssertions** - RAG特定断言
- **TestReportGenerator** - 测试报告生成

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行ES集成测试
./gradlew test --tests "Elasticsearch*"

# 查看测试报告
open build/reports/tests/test/index.html
```

### 测试覆盖
- ✅ Elasticsearch连接测试
- ✅ 向量索引管理测试
- ✅ KNN检索测试
- ✅ 混合检索测试
- ✅ 性能基准测试

详见：[docs/RAG_Test_Status_Dashboard.md](examples/docs/RAG_Test_Status_Dashboard.md)

## 📖 文档

### 项目文档
详细文档请查看 [OpenSpec文档目录](./openspec/)：

- 📋 [变更日志](./openspec/CHANGELOG.md) - 版本变更记录
- 🤝 [贡献指南](./openspec/CONTRIBUTING.md) - 如何贡献代码
- 📐 [系统架构](./openspec/specs/system-architecture.md) - 系统架构设计
- 📜 [文档管理规则](./openspec/DOC_MANAGEMENT_RULES.md) - 文档组织规范

### 测试文档
- [RAG测试执行日志](examples/docs/RAG_Test_Execution_Log_2026-06-07.md)
- [RAG测试状态看板](examples/docs/RAG_Test_Status_Dashboard.md)
- [RAG测试指南](examples/docs/RAG_Elasticsearch_Testing_Guide.md)

### 数据库设计
- Flyway迁移脚本位于 `src/main/resources/db/migration/`
- 核心表：知识库、知识点、FAQ、文档、分块、向量索引等（10个核心表）

### API文档
- RESTful API风格
- 统一响应格式 `ApiResponse<T>`
- 详细的API接口定义（待补充OpenAPI文档）

## 🎯 开发路线图

### ✅ 已完成
- [x] 分块模块（6种分块策略）
- [x] 向量化服务接口
- [x] BM25检索策略
- [x] 向量检索策略
- [x] Elasticsearch向量集成
- [x] 知识库基础实体
- [x] 测试基础设施

### 🔄 进行中
- [ ] 真实向量化API集成
- [ ] 完整的API Controller
- [ ] GraphRAG模块
- [ ] QA问答模块

### 📋 计划中
- [ ] 文档解析器（PDF、Word、Excel）
- [ ] 多租户支持
- [ ] API限流和监控
- [ ] 性能优化

## 🤝 贡献指南

欢迎贡献代码、提出问题或建议！

### 开发流程
1. Fork项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

### Commit规范
遵循约定式提交：
- `feat:` - 新功能
- `fix:` - 问题修复
- `docs:` - 文档更新
- `test:` - 测试相关
- `refactor:` - 重构

详见：[贡献指南](./openspec/CONTRIBUTING.md)

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- Notebook RAG Team

## 🙏 致谢

- Spring Boot团队
- Elasticsearch团队
- OpenAI/通义千问团队

---

**项目状态**: 🚧 活跃开发中
**最后更新**: 2026-06-07
**版本**: 0.1.0-SNAPSHOT
