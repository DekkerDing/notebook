# 测试执行和自动化规则

本文档定义Notebook项目中的测试执行流程和自动化规则，确保不需要每次重复提及即可自动执行。

## 测试执行规则

### 自动化测试触发条件

以下情况自动执行测试：

1. **代码提交前**
   - 运行单元测试
   - 运行集成测试
   - 验证编译通过

2. **PR合并前**
   - 运行完整测试套件
   - 生成测试覆盖率报告
   - 验证性能基准

3. **功能开发完成**
   - 编写对应测试用例
   - 验证功能正确性
   - 更新测试文档

### 测试优先级

| 优先级 | 测试类型 | 执行频率 | 说明 |
|-------|---------|---------|------|
| P0 | 单元测试 | 每次提交 | 核心逻辑测试 |
| P1 | 集成测试 | 每次提交 | 模块间交互测试 |
| P2 | 性能测试 | 每日 | 性能基准验证 |
| P3 | 端到端测试 | 发布前 | 完整流程验证 |

## 自动化测试流程

### 1. 开发阶段

```bash
# 编译验证
./gradlew compileJava

# 运行快速测试
./gradlew test --tests "*UnitTest"

# 运行特定模块测试
./gradlew test --tests "io.github.dekkerding.examples.domain.retrieval.*"
```

### 2. 提交前验证

```bash
# 运行所有测试
./gradlew test

# 检查测试覆盖率
./gradlew test jacocoTestReport

# 验证代码风格
./gradlew checkStyleMain
```

### 3. 持续集成

自动执行：
```yaml
# .github/workflows/test.yml
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - checkout
      - name: 编译
        run: ./gradlew compileJava
      - name: 测试
        run: ./gradlew test
      - name: 覆盖率
        run: ./gradlew jacocoTestReport
```

## 测试编写规范

### 单元测试

**位置**: `src/test/java/io/github/dekkerding/examples/{module}/`

**命名**: `{ClassName}Test.java`

**结构**:
```java
@SpringBootTest
class ClassNameTest {

    @Autowired
    private ClassName target;

    @BeforeEach
    void setUp() {
        // 初始化
    }

    @Test
    void testFeature() {
        // 测试逻辑
        assertNotNull(target);
    }
}
```

### 集成测试

**位置**: `src/test/java/io/github/dekkerding/examples/integration/`

**命名**: `{Feature}IntegrationTest.java`

**结构**:
```java
@SpringBootTest
@ActiveProfiles("test")
class FeatureIntegrationTest {

    @Autowired
    private FeatureService service;

    @Test
    void testFeatureIntegration() {
        // 集成测试逻辑
    }
}
```

## 测试数据管理

### 测试数据原则

1. **独立性**: 每个测试应有独立数据
2. **可重复性**: 测试结果应可重复
3. **最小化**: 使用最小必要数据集

### 测试数据位置

```
examples/
├── src/test/
│   ├── resources/
│   │   ├── fixtures/          # 测试数据
│   │   │   ├── documents.json
│   │   │   └── queries.json
│   │   └── test-config.yml    # 测试配置
```

## 性能测试规则

### 性能基准

| 功能 | 基准 | 容差 | 监控频率 |
|-----|------|------|---------|
| BM25检索 | <100ms | ±20ms | 每日 |
| 向量检索 | <200ms | ±50ms | 每日 |
| 多路融合 | <300ms | ±100ms | 每日 |
| 文档解析 | <500ms | ±200ms | 每周 |

### 性能回归检测

```bash
# 运行性能测试
./gradlew performanceTest

# 生成性能报告
./gradlew performanceReport
```

## 测试状态跟踪

### 测试状态分类

- ✅ **PASS**: 测试通过
- ❌ **FAIL**: 测试失败，需要修复
- ⏭️  **SKIP**: 测试跳过（条件不满足）
- ⚠️  **FLAKY**: 测试不稳定，需要调查
- 📝 **TODO**: 测试待实现

### 测试覆盖目标

| 模块 | 当前覆盖率 | 目标覆盖率 | 状态 |
|-----|-----------|-----------|------|
| 检索模块 | 85% | 90% | 🔄 进行中 |
| 分块模块 | 75% | 85% | ⚠️ 需改进 |
| 向量化模块 | 60% | 80% | ❌ 需补充 |

## 自动化规则执行

### Pre-commit Hook

```bash
# .git/hooks/pre-commit
#!/bin/bash

echo "🔍 运行测试前验证..."

# 编译检查
./gradlew compileJava || exit 1

# 快速测试
./gradlew test --tests "*FastTest" || exit 1

echo "✅ 验证通过"
```

### CI/CD自动化

- **Push**: 运行所有测试
- **PR**: 运行所有测试 + 覆盖率检查
- **Main分支**: 部署前运行完整测试 + 性能测试
- **Nightly**: 运行完整测试套件 + 生成报告

## 测试失败处理

### 处理流程

1. **分析失败原因**
   - 查看测试日志
   - 检查错误堆栈
   - 确定失败类型

2. **分类处理**
   - **代码问题**: 修复代码
   - **测试问题**: 修复测试
   - **环境问题**: 修复环境

3. **验证修复**
   - 运行失败测试
   - 运行相关测试
   - 确保无回归

### 失败阻断规则

- ❌ P0测试失败：阻断提交
- ⚠️  P1测试失败：警告但允许提交
- ℹ️  P2测试失败：记录但不阻断

## 测试文档规则

### 测试文档位置

```
openspec/
├── test-execution-status.md     # 测试执行状态
├── test-coverage-report.md      # 覆盖率报告（自动生成）
└── test-performance-report.md    # 性能测试报告（自动生成）
```

### 测试文档更新

- 测试状态变更时更新执行状态文档
- 每周生成覆盖率报告
- 每月生成性能测试报告

---

**文档状态**: Active
**创建日期**: 2026-06-07
**最后更新**: 2026-06-07
**维护者**: Notebook RAG Team
