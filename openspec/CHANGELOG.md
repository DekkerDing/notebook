# 变更日志

本文档记录Notebook项目的所有重要变更。

格式基于[Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循[语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 待发布

#### 检索功能增强
- **多路融合检索** - 并行执行多个检索策略并智能融合结果
  - 基于RRF（Reciprocal Rank Fusion）算法的结果融合
  - 自适应权重调整（根据查询类型：关键词/语义/关系）
  - 并行执行降低总体延迟至<300ms
- **混合检索协调器** - 统一的检索策略编排和管理
  - 策略动态选择和组合
  - 结果后处理（过滤、截断、排序）
  - 策略元查询接口
- **结果融合服务** - ResultFusionService
  - RRF算法实现
  - 智能去重和排序
  - 融合元数据记录

#### 架构与文档
- **设计决策文档** - 检索策略设计决策记录
  - 多路融合架构设计
  - 结果去重策略
  - 检索缓存策略
  - 结果截断策略
  - 策略优先级设计
- **测试执行规则** - 自动化测试流程规范
  - 测试优先级分类
  - 自动化触发条件
  - 性能基准定义
  - 失败处理流程
- **实现总结文档** - 高级检索功能实现完成总结

#### 测试完善
- 检索策略单元测试（15+测试用例）
- 结果融合服务测试
- BM25策略功能测试
- 查询类型识别测试

#### 之前已完成
- Elasticsearch索引性能优化（批量索引、并发索引、向量序列化）
- 文档解析功能（PDF、Word、Excel）
- GraphRAG知识图谱模块（图存储、实体/关系抽取、图查询）

---

## [0.1.0] - 2026-06-07

### 新增 (Added)

#### 核心功能
- **分块模块** - 实现6种文档分块策略
  - 分隔符分块 (SeparatorChunkStrategy)
  - 递归字符分块 (RecursiveChunkStrategy)
  - 语义分块 (SemanticChunkStrategy)
  - 表格分块 (TableChunkStrategy)
  - 层次标题分块 (计划中)
  - 代码分块 (计划中)
- **向量化服务** - 统一的向量化服务接口
  - 支持单个/批量向量化
  - 向量缓存管理
  - 多提供商支持（接口定义）
- **检索模块** - 实现多种检索策略
  - BM25稀疏检索 (BM25RetrieveStrategy)
  - 向量密集检索 (VectorRetrieveStrategy)
  - 混合检索（向量+BM25）
  - 多路融合检索（计划中）
- **知识库管理** - 完整的知识库CRUD功能
  - 知识库实体模型
  - 知识点实体模型
  - JPA Repository接口
  - 分类管理功能

#### 基础设施
- **Elasticsearch集成** - 基于ES 8.17的向量存储
  - 向量索引创建和管理
  - 文档向量索引（单个/批量）
  - KNN向量检索
  - 混合检索支持
  - RestTemplate配置
- **数据库设计** - 10个核心表设计
  - kb_knowledge_base（知识库表）
  - kb_knowledge_point（知识点表）
  - kb_faq_pair（FAQ表）
  - kb_conversation_log（对话日志表）
  - kb_document（文档表）
  - kb_chunk（分块表）
  - kb_vector_index（向量索引表）
  - kb_agent_record（Agent记录表）
  - kb_ticket（工单表）
  - kb_cache_event（缓存事件表）
- **Flyway迁移脚本** - 数据库版本管理

#### API接口
- **统一响应格式** - ApiResponse<T>
- **分页响应** - PageResult<T>
- **知识库管理API** (Controller定义)
  - 创建、列表、详情、更新、删除、归档
  - 批量操作、搜索、分类树
- **检索API** (Controller定义)
  - 搜索、相似度、多路检索、批量检索、向量检索

#### 测试基础设施
- **TestProgressTracker** - 测试进度追踪器
  - 测试套件管理
  - 阶段划分
  - 状态追踪
  - 性能指标收集
- **RagAssertions** - RAG特定断言框架
  - 基础断言方法
  - ES特定断言
  - 检索特定断言
  - 性能断言
- **TestReportGenerator** - 测试报告生成器
  - HTML格式报告
  - JSON格式报告
  - 文本格式报告

#### 测试套件
- **ElasticsearchConnectionTest** - ES连接测试 ✅
  - 配置验证（8个测试点）
  - HTTP连接测试（2个测试点）
- **ElasticsearchVectorIntegrationTest** - ES向量集成测试 ✅
  - 连接测试
  - 索引管理测试
  - 向量数据索引测试
  - KNN检索测试
  - 混合检索测试
  - 性能基准测试
  - 边界条件测试
  - 错误处理测试

#### 文档
- **项目文档**
  - README.md - 项目概述和快速开始
  - CONTRIBUTING.md - 贡献指南
  - CHANGELOG.md - 变更日志（本文件）
- **模块文档**
  - examples/README.md - 主模块详细文档
  - docs/RAG_Test_Execution_Log_2026-06-07.md - 测试执行日志
  - docs/RAG_Test_Status_Dashboard.md - 测试状态看板
  - docs/RAG_Elasticsearch_Testing_Guide.md - ES测试指南
  - docs/RAG_Elasticsearch_Test_Summary.md - ES测试总结

### 配置 (Changed)

#### 依赖更新
- 添加Spring Boot Web依赖
- 添加Spring Data JPA依赖
- 添加Spring Boot Validation依赖
- 添加Jackson依赖（2.13.5）
- 添加MySQL和H2数据库驱动
- 配置Lombok测试依赖

#### 构建配置
- Gradle依赖管理更新
- Java 8目标兼容性
- UTF-8编码配置

### 修复 (Fixed)

- 修复Java 8兼容性问题
  - 替换`Map.of()`为HashMap
  - 替换`List.of()`为Collections.emptyList()
  - 修复`Matcher`导入问题
- 修复Controller泛型语法错误
- 修复Lombok测试配置

### 性能 (Performance)

#### 测试基准性能
- 单文档索引延迟: <1000ms ✅
- 批量索引延迟: 2175ms (50文档) ✅
- KNN检索延迟: 155ms ✅
- 混合检索延迟: 61ms ✅

### 安全 (Security)

- 添加基础配置验证
- Elasticsearch连接超时配置
- 输入验证框架集成

---

## [0.0.1] - 之前版本

### 早期开发

- Java算法实验模块
- 排序算法实现
- 数据结构练习

---

## 版本说明

### 版本格式: MAJOR.MINOR.PATCH

- **MAJOR** - 不兼容的API变更
- **MINOR** - 向后兼容的功能新增
- **PATCH** - 向后兼容的问题修复

### 变更类型

- **新增 (Added)** - 新功能
- **变更 (Changed)** - 现有功能的变更
- **弃用 (Deprecated)** - 即将移除的功能
- **移除 (Removed)** - 已移除的功能
- **修复 (Fixed)** - 问题修复
- **安全 (Security)** - 安全相关的修复

---

**更新日期**: 2026-06-07
**维护者**: Notebook RAG Team
