# OpenSpec文档管理规则

本文档定义Notebook项目中所有描述性文档的统一管理规则。

## 文档位置规则

### 原则
所有描述性、规范性的Markdown文档应统一放置在`/openspec`目录下进行管理，项目根目录仅保留`README.md`作为项目主入口。

### 目录结构

```
notebook/
├── README.md                    # 项目主入口（保留在根目录）
├── openspec/
│   ├── DOC_MANAGEMENT_RULES.md  # 本文档 - 文档管理规则
│   ├── CONTRIBUTING.md          # 贡献指南
│   ├── CHANGELOG.md             # 变更日志
│   ├── config.yaml              # OpenSpec配置
│   ├── specs/                   # 规范文档目录
│   │   └── system-architecture.md
│   └── changes/                 # 变更目录
│       └── [change-name]/
└── examples/
    └── README.md               # 主模块文档（保留在模块目录）
```

## 文档分类规则

### 1. 规范类文档（/openspec/specs/）

**用途**: 定义系统的技术规范、架构设计、API接口等

**包含**:
- 系统架构规范 (system-architecture.md)
- API接口规范
- 数据模型规范
- 安全规范
- 性能规范

**命名规范**: `{component}-{type}-spec.md`

### 2. 变更类文档（/openspec/changes/[change-name]/）

**用途**: 记录每个变更的完整生命周期

**包含**:
- proposal.md - 变更提案
- design.md - 设计文档
- specs/ - 规范文档
- tasks.md - 任务清单

**命名规范**: kebab-case（如 `implement-graphrag`）

### 3. 项目管理类文档（/openspec/）

**用途**: 跨项目的通用管理文档

**包含**:
- CHANGELOG.md - 变更日志
- CONTRIBUTING.md - 贡献指南
- DOC_MANAGEMENT_RULES.md - 文档管理规则（本文档）
- RELEASE_NOTES.md - 发布说明（如有）

### 4. 模块文档（/examples/或其他模块目录/）

**用途**: 特定模块的详细文档

**包含**:
- 各模块的README.md
- 模块使用指南
- 模块开发指南

**位置**: 保持在各模块根目录

### 5. 项目主入口（根目录）

**用途**: 项目整体介绍和导航

**包含**:
- README.md（仅此一个）

## 文档创建规则

### 新增规范文档时

```bash
# 1. 在openspec/specs/下创建
openspec/specs/[component]-[type]-spec.md

# 2. 在README.md中添加索引链接
```

### 新增变更时

```bash
# 使用OpenSpec命令
openspec new change "[change-name]"

# 或使用技能
/opsx:propose "change description"
```

### 新增项目管理文档时

```bash
# 放置在openspec/目录
openspec/[DOCUMENT_NAME].md
```

### 更新模块文档时

```bash
# 在对应模块目录下更新
examples/README.md
interface/README.md
```

## 文档引用规则

### README.md中的引用

```markdown
## 文档

详细文档请查看 [OpenSpec文档目录](./openspec/)：

- 📋 [变更日志](./CHANGELOG.md)
- 🤝 [贡献指南](./CONTRIBUTING.md)
- 📐 [系统架构](./specs/system-architecture.md)
```

### 文档间的交叉引用

使用相对路径：

```markdown
# 在openspec/下的文档中引用
参见[系统架构](./specs/system-architecture.md)
参见[变更日志](./CHANGELOG.md)

# 在changes/下的文档中引用openspec/文档
参见[贡献指南](../CONTRIBUTING.md)
参见[系统架构](../specs/system-architecture.md)
```

## 文档迁移规则

### 根目录文档 → openspec/

以下文档类型应从根目录移动到`openspec/`：

- ❌ 不应在根目录: CHANGELOG.md, CONTRIBUTING.md, RELEASE_NOTES.md
- ✅ 应在: openspec/CHANGELOG.md, openspec/CONTRIBUTING.md

### 例外情况

**仅保留在根目录**:
- ✅ README.md（项目主入口）

**保留在各模块目录**:
- ✅ {module}/README.md（模块文档）

## 文档内容规则

### CHANGELOG.md

- 位置: `openspec/CHANGELOG.md`
- 格式: 基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)
- 更新时机: 每次发布或重大功能合并时

### CONTRIBUTING.md

- 位置: `openspec/CONTRIBUTING.md`
- 内容: 贡献流程、代码规范、提交信息规范
- 更新时机: 贡献流程变化时

### README.md

- 位置: 根目录
- 内容: 项目概述、快速开始、主要功能链接
- 更新时机: 项目结构或主要功能变化时

## 自动化规则执行

### 检查脚本

创建检查脚本验证文档位置：

```bash
# scripts/check-doc-locations.sh
# 检查是否有文档应放在openspec/但位于根目录
```

### Pre-commit Hook

在提交前检查文档位置：

```bash
# .git/hooks/pre-commit
# 拒绝违反文档放置规则的提交
```

## 生成的文档遵循规则

### OpenSpec生成的文档

所有OpenSpec工作流生成的文档应遵循：

1. **变更文档** → `openspec/changes/[change-name]/`
2. **规范文档** → `openspec/specs/`
3. **任务文档** → `openspec/changes/[change-name]/tasks.md`

### 自动化工具生成

使用以下工具生成文档时，确保输出到正确位置：

```yaml
# 工具配置示例
output:
  specs: "openspec/specs/"
  changes: "openspec/changes/"
  docs: "openspec/"
```

## 规则执行清单

在新文档创建或迁移时，使用此清单确认：

- [ ] 确定文档类型（规范/变更/管理/模块）
- [ ] 选择正确目录位置
- [ ] 使用正确的命名规范
- [ ] 更新相关索引/链接
- [ ] 如需要，从旧位置删除
- [ ] 提交时遵循commit message规范

## 违规处理

### 发现违规文档位置时

1. 移动文档到正确位置
2. 更新所有引用链接
3. 在PR中说明迁移原因
4. 确保CI检查通过

### 示例

```bash
# 错误：CHANGELOG.md在根目录
notebook/CHANGELOG.md

# 正确：移动到openspec/
notebook/openspec/CHANGELOG.md
```

## 版本历史

- v1.0.0 (2026-06-07) - 初始版本，定义基础规则

---

**维护者**: Notebook RAG Team
**更新日期**: 2026-06-07
