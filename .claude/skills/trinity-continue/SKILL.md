---
name: trinity-continue
description: 继续变更（带追踪）- 三段式调用 planning-with-files + OpenSpec CLI。使用场景：用户想要继续 OpenSpec 变更工作流，创建下一个产出物。
license: MIT
compatibility: 需要 openspec CLI 和 planning-with-files skill
metadata:
  author: trinity
  version: "2.2"
  generatedBy: "trinity-workflow-v2"
---

# trinity:continue - 继续变更

> **Trinity Workflow v2** - 三段式调用：planning-with-files → OpenSpec CLI → planning-with-files

---

## 追踪文件位置

```
⚠️ 重要：追踪文件必须放在变更目录内，而非项目根目录

正确位置: openspec/changes/{change-id}/
  ├── task_plan.md      # 阶段进度
  ├── findings.md       # 技术发现
  ├── progress.md       # 操作日志
  └── delta-log.md      # 规格变更记录

错误位置: {project-root}/
  ├── task_plan.md      # ❌ 不要放这里
  ├── findings.md       # ❌ 不要放这里
  └── progress.md       # ❌ 不要放这里
```

---

## 触发

用户调用: `/trinity:continue`

---

## 执行流程

### Phase 1: 调用 planning-with-files（前置）

```
[MUST] 首先调用 planning-with-files skill 进行上下文加载
```

Use the Skill tool with skill: "planning-with-files"

**目的**:
1. 读取 `openspec/.active` 获取当前 change-id
2. 如果没有活跃变更，提示用户先运行 `/trinity:new`
3. 读取追踪文件了解当前状态:
    - `openspec/changes/{change-id}/task_plan.md` - 当前阶段
    - `openspec/changes/{change-id}/progress.md` - 历史操作
    - `openspec/changes/{change-id}/findings.md` - 已做决策

---

### Phase 2: 执行 OpenSpec CLI

#### 2.1 获取当前状态

```bash
openspec status --change "<change-id>" --json
```

**解析 JSON 获取**:
- `schemaName`: 当前使用的 schema
- `artifacts`: artifacts 数组，包含每个 artifact 的状态
    - `id`: artifact ID (proposal, specs, design, tasks)
    - `status`: "done" | "ready" | "blocked"
- `isComplete`: 是否所有 artifacts 已完成

#### 2.2 确定下一个 artifact

```
遍历 artifacts 数组:
  找到第一个 status === "ready" 的 artifact
  这是下一个要创建的 artifact
```

**处理特殊情况**:
- 如果 `isComplete: true`: 提示用户所有 artifacts 已完成，可以运行 `/trinity:apply`
- 如果所有 artifacts 都是 "blocked": 检查依赖关系，提示用户

#### 2.3 获取 artifact 指令

```bash
openspec instructions <artifact-id> --change "<change-id>" --json
```

**解析 JSON 获取**:
- `context`: 项目背景信息
- `rules`: artifact 特定规则
- `template`: artifact 模板结构
- `instruction`: schema 特定指导
- `outputPath`: 输出文件路径
- `dependencies`: 需要读取的依赖文件

#### 2.4 读取依赖文件

```
根据 dependencies 字段读取已完成的相关 artifacts:
- 如果创建 specs: 读取 proposal.md
- 如果创建 design: 读取 proposal.md, specs/
- 如果创建 tasks: 读取 proposal.md, specs/, design.md
```

#### 2.5 创建 artifact

基于 `template` 和 `instruction` 创建 artifact:
1. 使用模板结构
2. 应用 context 和 rules 作为约束
3. 写入 outputPath 指定的路径

---

### Phase 3: 调用 planning-with-files（后置）

```
[MUST] 最后调用 planning-with-files skill 更新追踪文件
```

Use the Skill tool with skill: "planning-with-files"

**传递以下信息用于更新追踪**:
- 创建的 artifact 名称
- CLI 返回的依赖信息
- 操作日志

**追踪文件更新内容**:

| 创建的 Artifact | task_plan.md 更新 | progress.md 更新 | findings.md 更新 |
|----------------|-------------------|-------------------|-------------------|
| proposal.md | Phase: proposal, Progress: 20% | "✓ proposal.md 创建" | 提取关键决策 |
| specs/*.md | Phase: specs, Progress: 40% | "✓ specs/ 创建" | Delta 记录到 delta-log.md |
| design.md | Phase: design, Progress: 60% | "✓ design.md 创建" | 提取 ADR |
| tasks.md | Phase: tasks, Progress: 80% | "✓ tasks.md 创建" | 同步任务清单 |

---

## 输出格式

```
✅ Artifact 创建完成

📄 创建: {artifact-name}
📊 当前进度: {百分比}%
📋 当前阶段: {阶段名}

📝 追踪文件已更新:
   ✓ task_plan.md - 阶段更新
   ✓ progress.md - 操作日志

🚀 下一步: {下一步操作}
```

---

## 使用示例

```bash
/trinity:continue
> 创建 proposal.md...

/trinity:continue
> 创建 specs/ 目录...
```

---

## 架构说明

```
┌─────────────────────────────────────────────────────────────────┐
│  trinity:continue 三段式架构                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Phase 1: planning-with-files (前置)                            │
│  → 读取活跃变更、追踪文件状态                                     │
│                                                                  │
│  Phase 2: OpenSpec CLI                                          │
│  → openspec status --json (获取下一个 artifact)                  │
│  → openspec instructions --json (获取创建规则)                   │
│  → 基于 rules + template 创建 artifact                          │
│                                                                  │
│  Phase 3: planning-with-files (后置)                            │
│  → 更新 task_plan.md, progress.md, findings.md                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**关键原则**:
- 不侵入修改 planning-with-files skill
- 不侵入修改 OpenSpec 官方 skills
- 通过调用实现集成，而非内联逻辑