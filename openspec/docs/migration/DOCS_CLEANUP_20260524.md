# 文档整理记录 (2026-05-24)

## 整理概述

将项目中散落在各处的.md文档统一归类到 `openspec/` 目录进行集中管理。

---

## 文件移动记录

### 从项目根目录移动

| 原路径 | 新路径 | 类型 |
|--------|--------|------|
| `DEPLOYMENT.md` | `openspec/docs/deployment/DEPLOYMENT.md` | 部署文档 |
| `DEPLOYMENT_CHECKLIST.md` | `openspec/docs/deployment/DEPLOYMENT_CHECKLIST.md` | 部署文档 |
| `OPTIMIZATION_GUIDE.md` | `openspec/docs/optimization/OPTIMIZATION_GUIDE.md` | 优化文档 |
| `MIGRATION_SUMMARY.md` | `openspec/docs/migration/MIGRATION_SUMMARY.md` | 迁移文档 |
| `Task.md` | `openspec/tasks/Task.md` | 任务记录 |
| `AGENTS.md` | `openspec/ai-collaboration/AGENTS.md` | AI协作文档 |

### 从 examples/ 目录移动

| 原路径 | 新路径 | 类型 |
|--------|--------|------|
| `examples/TEST_EXECUTION_GUIDE.md` | `openspec/tests/TEST_EXECUTION_GUIDE.md` | 测试文档 |
| `examples/TEST_PLAN.md` | `openspec/tests/TEST_PLAN.md` | 测试文档 |
| `examples/TEST_REPORT.md` | `openspec/tests/TEST_REPORT.md` | 测试文档 |

### 保留在根目录

| 文件 | 原因 |
|------|------|
| `README.md` | 项目入口文档 |
| `CLAUDE.md` | AI配置文件 |

---

## 新增目录结构

```
openspec/
├── INDEX.md                    # 文档索引（新增）
├── docs/
│   ├── design/                 # 新增目录
│   └── guide/                  # 新增目录
└── tests/                      # 新增目录
    ├── TEST_EXECUTION_GUIDE.md
    ├── TEST_PLAN.md
    └── TEST_REPORT.md
```

---

## 配置更新

### 1. CLAUDE.md 更新

新增 **"Markdown 文件管理规范"** 章节，包含：

- 强制路径约束：禁止在examples/、根目录等位置创建.md文档
- 文件名模式自动识别：根据文件名自动判断归类
- 详细归类映射表：覆盖所有文档类型

**关键约束**：
```markdown
# ❌ 禁止的路径
Write: examples/TEST_*.md
Write: TEST_*.md
Write: docs/测试*.md

# ✅ 正确的路径
Write: openspec/tests/TEST_*.md
Write: openspec/docs/design/*_DESIGN.md
```

### 2. openspec/INDEX.md 更新

- 新增完整的目录结构说明
- 新增文档归类映射表
- 新增按场景查找文档的指南
- 新增测试文档索引

### 3. README.md 更新

- 更新所有文档链接指向新的openspec/路径
- 新增"文档中心"章节

---

## 后续生成规则

### 测试文档

任何与测试相关的.md文档将**强制**归类到 `openspec/tests/`：

| 文件名模式 | 自动归类 |
|-----------|---------|
| `TEST_*.md` | `openspec/tests/` |
| `*_TEST.md` | `openspec/tests/` |
| `测试*.md` | `openspec/tests/` |
| `*_EXECUTION_GUIDE.md` | `openspec/tests/` |
| `REGRESSION_*.md` | `openspec/tests/` |
| `PERFORMANCE_*.md` | `openspec/tests/` |

### 其他文档类型

| 类型 | 文件名模式 | 目标目录 |
|------|-----------|---------|
| 设计文档 | `*_DESIGN.md`、`设计*.md` | `openspec/docs/design/` |
| 部署文档 | `DEPLOYMENT*.md` | `openspec/docs/deployment/` |
| 优化文档 | `OPTIMIZATION*.md` | `openspec/docs/optimization/` |
| 任务记录 | `Task*.md`、`任务*.md` | `openspec/tasks/` |

---

## 效果验证

### 验证1：目录清洁

```bash
# 项目根目录应仅有2个.md文件
$ ls *.md
CLAUDE.md
README.md

# examples/目录应无.md文件
$ ls examples/*.md
ls: cannot access 'examples/*.md': No such file
```

### 验证2：文档归类

```bash
# 所有文档都在openspec/下
$ find openspec/ -name "*.md" | wc -l
18
```

---

## 收益

1. **目录清洁**：项目根目录仅保留必要的README和CLAUDE配置
2. **易于查找**：所有文档按类型分类，快速定位
3. **自动归类**：后续生成文档自动归类，无需手动整理
4. **一致性**：统一的文档结构和命名规范

---

**整理日期**: 2026-05-24
**整理人**: Redis工具集团队
**版本**: v1.0
