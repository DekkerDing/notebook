# 贡献指南

感谢您对Notebook项目的关注！我们欢迎任何形式的贡献。

## 🤝 如何贡献

### 报告问题

如果您发现了bug或有功能建议：

1. 检查[Issues](../../issues)是否已有相同问题
2. 如果没有，创建新的Issue
3. 提供详细的问题描述和重现步骤

**问题报告模板**：

```markdown
## 问题描述
简要描述遇到的问题

## 环境信息
- OS版本:
- JDK版本:
- 项目版本:
- 其他相关信息:

## 重现步骤
1. 步骤1
2. 步骤2
3. ...

## 期望行为
描述您期望发生什么

## 实际行为
描述实际发生了什么

## 日志输出
粘贴相关日志
```

### 提交代码

#### 准备工作

1. **Fork项目**并克隆到本地

```bash
git clone <your-fork-url>
cd notebook
```

2. **添加上游仓库**

```bash
git remote add upstream https://github.com/original-repo/notebook.git
```

3. **创建特性分支**

```bash
git checkout -b feature/your-feature-name
```

#### 开发规范

##### 代码风格

- 遵循Java代码规范
- 使用有意义的变量和方法名
- 添加必要的注释说明复杂逻辑
- 保持代码简洁，避免过度设计

##### Commit规范

使用[约定式提交](https://www.conventionalcommits.org/)格式：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型 (type)**：
- `feat`: 新功能
- `fix`: 问题修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具链相关

**示例**：

```bash
git commit -m "feat(retrieval): 添加混合检索支持

实现了向量检索和BM25检索的混合策略，
支持权重配置和结果融合。

Closes #123"
```

##### 测试要求

- 为新功能添加单元测试
- 确保测试覆盖率不降低
- 运行完整测试套件：

```bash
cd examples
./gradlew test
```

##### 文档要求

- 更新相关文档
- 为公共API添加JavaDoc
- 更新README（如需要）

#### 提交流程

1. **推送代码**

```bash
git push origin feature/your-feature-name
```

2. **创建Pull Request**

- 描述您的更改
- 关联相关Issue
- 确保CI检查通过
- 等待代码审查

3. **响应反馈**

- 及时回应审查意见
- 进行必要的修改
- 保持讨论礼貌和专业

## 📋 开发指南

### 项目结构

```
notebook/
├── examples/              # 主应用模块
│   ├── src/main/java/
│   │   ├── domain/        # 领域层（核心业务逻辑）
│   │   ├── infrastructure/# 基础设施层
│   │   ├── interfaces/    # 接入层（Controller）
│   │   └── common/       # 公共组件
│   ├── src/test/java/    # 测试代码
│   └── docs/             # 模块文档
├── openspec/             # 规范文档
└── notes/                # 学习笔记
```

### 技术栈

- **语言**: Java 8
- **框架**: Spring Boot 2.6.14
- **构建**: Gradle 7.6.4
- **测试**: JUnit 5, Spring Boot Test
- **数据库**: MySQL 8.0, Elasticsearch 8.17, Redis

### 开发环境设置

#### 必需工具

- JDK 8+
- Gradle 7.x
- IDE (IntelliJ IDEA推荐)
- Git

#### 可选工具

- Docker (运行Elasticsearch和MySQL)
- Postman (API测试)

#### 本地服务

**使用Docker启动依赖服务**：

```bash
# Elasticsearch
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.17.0

# MySQL
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=notebook \
  mysql:8.0

# Redis
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:6-alpine
```

### 构建和运行

```bash
# 构建项目
cd examples
./gradlew clean build

# 运行应用
./gradlew bootRun

# 运行测试
./gradlew test

# 生成测试报告
./gradlew test jacocoTestReport
```

### 调试技巧

#### 远程调试

```bash
./gradlew bootRun --args='--agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'
```

#### 日志配置

在 `application.yml` 中配置日志级别：

```yaml
logging:
  level:
    io.github.dekkerding.examples: DEBUG
    org.springframework: INFO
    org.elasticsearch: WARN
```

## 🧪 测试指南

### 测试策略

- **单元测试**: 测试单个类或方法
- **集成测试**: 测试模块间交互
- **端到端测试**: 测试完整业务流程

### 测试示例

```java
@SpringBootTest
class ElasticsearchVectorStoreTest {

    @Autowired
    private ElasticsearchVectorStore vectorStore;

    @Test
    void shouldIndexDocument() {
        // Given
        String docId = "test-doc";
        String content = "测试内容";
        float[] vector = generateTestVector(1536);

        // When
        String result = vectorStore.indexDocument(docId, content, vector, null);

        // Then
        assertNotNull(result);
        assertEquals(docId, result);
    }
}
```

### 测试工具

项目提供了测试基础设施：

- **TestProgressTracker** - 测试进度追踪
- **RagAssertions** - RAG特定断言

## 📖 文档指南

### 文档结构

- `README.md` - 项目概述
- `CONTRIBUTING.md` - 贡献指南（本文件）
- `CHANGELOG.md` - 变更日志
- `examples/README.md` - 主模块文档
- `examples/docs/` - 详细技术文档

### 编写文档

- 使用清晰的Markdown格式
- 提供代码示例
- 包含必要的图表
- 保持文档与代码同步

## 🎯 功能开发流程

### 1. 需求讨论

在Issue中讨论新功能的设计和实现方案。

### 2. 创建设计文档

对于较大功能，创建设计文档：

```markdown
# 功能名称

## 背景
描述功能背景和原因

## 目标
描述功能和目标

## 设计方案
详细的技术设计

## 实现计划
分步骤的实现计划

## 测试计划
如何测试这个功能
```

### 3. 分工实施

按照设计文档分工实施：
- 创建特性分支
- 编写代码和测试
- 更新文档

### 4. 代码审查

提交PR进行代码审查：
- 功能完整性
- 代码质量
- 测试覆盖
- 文档更新

### 5. 合并发布

审查通过后合并到主分支。

## 🐛 问题修复流程

### 1. 确认问题

在Issue中确认问题是否存在。

### 2. 定位原因

分析代码，定位问题根因。

### 3. 编写测试

编写能重现问题的测试用例。

### 4. 修复问题

修复代码并确保测试通过。

### 5. 提交PR

提交PR，描述问题和修复方案。

## 📜 发布流程

### 版本号规范

遵循[语义化版本](https://semver.org/)：

- `MAJOR.MINOR.PATCH`
- `MAJOR`: 不兼容的API变更
- `MINOR`: 向后兼容的功能新增
- `PATCH`: 向后兼容的问题修复

### 发布步骤

1. 更新版本号
2. 更新CHANGELOG
3. 创建Git Tag
4. 构建发布包
5. 部署到仓库

## 💬 交流渠道

- **Issues**: 问题报告和功能讨论
- **Pull Requests**: 代码审查
- **Discussions**: 一般性讨论

## 🌟 贡献者

感谢所有贡献者！

## 📄 许可证

贡献的代码将采用项目的MIT许可证。

---

**最后更新**: 2026-06-07
