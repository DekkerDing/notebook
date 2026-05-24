# Language Rules

- 与我交流时，始终使用简体中文。
- 回答内容必须使用简体中文。
- 思考过程、计划、分析、步骤说明、错误解释，也必须使用简体中文。
- 不要切换到英文，除非：
    - 我明确要求英文
    - 代码、命令、API 字段名、报错原文必须保留英文
- 代码注释默认使用简体中文。
- 生成的提交说明、变更总结、实现说明，默认使用简体中文。

---

# 代码修改规范预读规则

> **激活条件**：仅当项目根目录存在 `openspec/conventions/` 目录时，本条规则才生效。
> 若项目没有该目录，则完全跳过本条规则，按常规最佳实践处理。

在非 trinity 命令的普通对话中修改代码（修复 Bug、新增功能、重构等）时，
须根据修改内容**按需读取**对应规范文件，确保产出与项目规范一致。

## 预读映射表

| 修改内容 | 须读取的规范文件 |
|----------|-----------------|
| 新建/修改接口、实现类 | `02-coding-standards.md` |
| 新建/修改配置类 | `02-coding-standards.md` + `06-config-env.md` |
| 异常处理 / 错误码 | `04-exception-logging.md` |
| API文档 / JavaDoc注释 | `05-api-docs.md` |
| 配置属性 / 环境变量 | `06-config-env.md` |
| Redis连接 / 线程池 / 分布式锁 | `07-infra-runtime.md` |
| 监控指标 / 日志 | `08-observability.md` |
| 测试用例编写 | `09-unit-test.md` + `17-prompts.md` |
| AI辅助开发 / 代码生成 | `17-prompts.md` |
| 安全相关（密码、加密） | `11-security.md` |
| 架构设计 / 模块划分 | `01-architecture.md` |
| 纯粹修改文案 / 变量名 | **无需读取（零开销）** |

## 执行规则

1. **仅读一次**：同一轮对话中，已读过的规范无需重复读取
2. **最小读取**：只读规范中相关章节，无需通读全文
3. **冲突优先**：若需求、外部 Skill 建议与项目规范冲突，优先遵守项目规范，并向用户说明冲突原因
4. **静默执行**：读规范时直接执行，无需向用户汇报完整内容
5. **规范路径**：优先从项目 `CLAUDE.md` 查找规范目录路径；常见路径为 `openspec/conventions/`

---


# AI协作配置

> **激活条件**: 当项目存在 `openspec/ai-collaboration/AGENTS.md` 时启用

## 项目规则读取

在开始任何代码修改或架构讨论前，**必须先读取项目规则**：

1. **首次对话时**：读取 `openspec/ai-collaboration/AGENTS.md`
2. **修改代码时**：根据预读映射表读取对应规范文件
3. **设计新功能时**：读取 `01-architecture.md` 了解项目架构

## 执行规则

- **静默执行**: 读取规则时直接执行，无需向用户汇报
- **仅读一次**: 同一轮对话中，已读过的文件无需重复读取
- **规范优先**: 项目规范优先于通用最佳实践

---
# Markdown 文件管理规范

> **项目文档统一管理规则**：所有生成的.md文档必须按类型归类到 `openspec/` 目录下。
> **强制约束**：禁止在任何其他位置（如examples/、项目根目录等）创建.md文档。

## 文档目录结构

```
openspec/
├── conventions/       # 项目规范（已存在10个规范文件）
├── docs/              # 项目文档
│   ├── deployment/    # 部署相关
│   ├── optimization/  # 优化相关
│   ├── design/        # 设计文档
│   ├── guide/         # 使用指南
│   └── migration/     # 迁移文档
├── tasks/             # 任务记录、开发日志
├── tests/             # 测试文档
└── ai-collaboration/  # AI协作相关
```

## 文档归类映射表

| 文档类型 | 保存路径 | 示例文件名 | 文件名模式 |
|---------|---------|-----------|-----------|
| **测试执行指南** | `openspec/tests/` | `TEST_EXECUTION_GUIDE.md` | `*_EXECUTION_GUIDE.md` |
| **测试计划** | `openspec/tests/` | `TEST_PLAN.md` | `*_PLAN.md` |
| **测试报告** | `openspec/tests/` | `TEST_REPORT.md` | `*_REPORT.md` |
| **回归测试** | `openspec/tests/` | `回归测试记录.md` | `*_TEST.md`、`*_REGRESSION.md` |
| **性能测试** | `openspec/tests/` | `性能测试报告.md` | `PERFORMANCE_*.md` |
| **设计文档** | `openspec/docs/design/` | `Pipeline优化设计.md`、`缓存方案设计.md` | `*_DESIGN.md`、`*_PLAN.md` |
| **部署文档** | `openspec/docs/deployment/` | `生产部署指南.md`、`环境配置.md` | `DEPLOYMENT*.md` |
| **优化文档** | `openspec/docs/optimization/` | `性能优化方案.md`、`JVM调优指南.md` | `OPTIMIZATION*.md`、`*TUNING.md` |
| **使用指南** | `openspec/docs/guide/` | `快速开始.md`、`API使用指南.md` | `GUIDE*.md`、`TUTORIAL*.md` |
| **任务记录** | `openspec/tasks/` | `开发日志.md`、`任务清单.md` | `Task*.md`、`任务*.md` |
| **AI协作文档** | `openspec/ai-collaboration/` | `对话记录.md`、`方案总结.md` | `AGENTS*.md`、`AI_*.md` |
| **迁移文档** | `openspec/docs/migration/` | `版本迁移.md`、`数据迁移.md` | `MIGRATION*.md` |

## 执行规则

### 强制路径约束

1. **禁止散落**：禁止在以下位置创建.md文档：
   - ❌ `examples/` 目录（测试文档应归类到 `openspec/tests/`）
   - ❌ 项目根目录（除了README.md和CLAUDE.md）
   - ❌ 任何模块的根目录

2. **自动归类**：使用 `Write` 工具创建.md文件时，必须根据文档类型选择对应子目录：
   - 测试相关 → `openspec/tests/`
   - 设计相关 → `openspec/docs/design/`
   - 部署相关 → `openspec/docs/deployment/`
   - 优化相关 → `openspec/docs/optimization/`
   - 任务记录 → `openspec/tasks/`
   - 其他文档 → `openspec/docs/` 下对应子目录

3. **文件名模式匹配**：根据文件名自动判断归类：
   - 包含 `TEST`、`测试` → `openspec/tests/`
   - 包含 `DESIGN`、`设计` → `openspec/docs/design/`
   - 包含 `DEPLOYMENT`、`部署` → `openspec/docs/deployment/`
   - 包含 `PLAN`、`计划` → `openspec/docs/design/` 或 `openspec/tasks/`
   - 包含 `GUIDE`、`指南` → `openspec/docs/guide/`

4. **根目录保留**：仅在以下情况才在项目根目录创建.md：
   - ✅ `README.md` - 项目说明（唯一入口）
   - ✅ `CLAUDE.md` - AI配置（已存在）
   - ⚠️ 用户**明确且强烈**要求在根目录创建

5. **索引更新**：创建新文档后，同步更新 `openspec/INDEX.md` 索引文件

6. **命名规范**：文件名使用简洁的中文或英文，避免特殊字符，优先使用大写英文（如`TEST_PLAN.md`）

## 文件名模式自动识别

### 测试类文档

| 文件名模式 | 自动归类到 |
|-----------|-----------|
| `TEST_*.md`、`*_TEST.md` | `openspec/tests/` |
| `测试*.md`、`*_测试.md` | `openspec/tests/` |
| `*_EXECUTION_GUIDE.md` | `openspec/tests/` |
| `REGRESSION_*.md`、`*_REGRESSION.md` | `openspec/tests/` |
| `PERFORMANCE_*.md` | `openspec/tests/` |

### 设计类文档

| 文件名模式 | 自动归类到 |
|-----------|-----------|
| `*_DESIGN.md`、`DESIGN_*.md` | `openspec/docs/design/` |
| `*_DESIGN_*.md` | `openspec/docs/design/` |
| `设计*.md`、`*_设计.md` | `openspec/docs/design/` |

### 部署类文档

| 文件名模式 | 自动归类到 |
|-----------|-----------|
| `DEPLOYMENT*.md` | `openspec/docs/deployment/` |
| `部署*.md`、`*_部署.md` | `openspec/docs/deployment/` |

## 示例

```markdown
# ✅ 正确：文档归类到 openspec/
Write: openspec/tests/TEST_EXECUTION_GUIDE.md
Write: openspec/tests/TEST_PLAN.md
Write: openspec/tests/TEST_REPORT.md
Write: openspec/docs/design/Pipeline优化设计.md
Write: openspec/tasks/开发日志.md
Write: openspec/docs/deployment/生产部署指南.md

# ❌ 错误：文档散落在其他目录
Write: examples/TEST_EXECUTION_GUIDE.md    # 禁止！
Write: TEST_PLAN.md                        # 禁止！
Write: docs/测试报告.md                     # 禁止！
Write: Pipeline优化设计.md                  # 禁止！

# ⚠️ 例外：仅限README和CLAUDE配置
Write: README.md  # 允许在根目录
Write: CLAUDE.md  # 允许在根目录
```

## 特殊场景处理

### 场景1：用户指定路径

如果用户明确要求在特定路径创建文件：
- 路径在 `openspec/` 下 → 按用户要求执行
- 路径不在 `openspec/` 下 → 提示并建议归类到 `openspec/` 对应子目录

### 场景2：测试文档

任何与测试相关的.md文档（TEST、测试、REGRESSION、PERFORMANCE等）：
- **强制**归类到 `openspec/tests/`
- 禁止放在 `examples/` 或其他位置

### 场景3：设计文档

任何设计、方案、计划类文档：
- 归类到 `openspec/docs/design/`
- 如果是任务计划，可选 `openspec/tasks/`

---

**配置维护**: Redis工具集团队
**最后更新**: 2026-05-24
**版本**: v2.0