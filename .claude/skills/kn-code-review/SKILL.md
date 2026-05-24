---
name: kn-code-review
description: 项目代码审查 - 支持前后端六阶段分层审查，可按 phase/module/file 定点审查
license: MIT
compatibility: 需要项目规范文件 openspec/conventions/
metadata:
  author: knowledge-system
  version: "3.0"
---

# kn-code-review - 全栈代码审查

> 六阶段分层审查：架构设计 → 数据库设计 → 代码规范 → 业务逻辑 → 安全优化 → 测试与可观测

---

## 触发

用户调用: `/kn-code-review`

可选参数：
- `--target frontend|backend|full` - 审查目标（默认: full）
- `--phase 1|2|3|4|5|6` - 仅执行指定阶段（默认: 全部）
- `--module <name>` - 指定审查模块
- `--file <path>` - 仅审查指定文件
- `--strict` - 包含推荐项检查（默认: 仅强制项）

---

## 统一审查范围

| 阶段 | 后端审查内容 | 前端审查内容 |
|------|-------------|-------------|
| **Phase 1** | DDD分层架构、包结构 | 前端架构、目录结构、分层设计 |
| **Phase 2** | 数据库设计、Flyway脚本 | 组件开发、样式设计、UI/UX 规范 |
| **Phase 3** | 命名规范、Controller/Service/DTO/PO | TS/React 编码规范、命名、导入 |
| **Phase 4** | 异常处理、日志、事务、配置 | 状态管理、API 层、路由 |
| **Phase 5** | 认证安全、输入验证、敏感数据 | XSS 防护、敏感信息、CSRF、性能 |
| **Phase 6** | 单元测试、集成测试、可观测性 | TypeScript 类型、测试、ESLint、国际化 |

---

## 执行流程

### Step 1: 解析参数

根据 `--target` 和 `--phase` 参数确定审查范围：

| target | phase | 加载规则文件 |
|--------|-------|-------------|
| full | - | 所有后端 + 前端 Phase 1-6 |
| full | 1-6 | 所有后端 + 前端指定阶段 |
| backend | - | 所有后端 Phase 1-6 |
| backend | 1-6 | 指定后端阶段 |
| frontend | - | 所有前端 Phase 1-6 |
| frontend | 1-6 | 指定前端阶段 |

### Step 2: 加载规则

根据 `--file` 和 `--module` 过滤审查目标：

- `--file`: 仅审查指定文件，加载对应规则
- `--module`: 仅审查指定模块下的文件
- 均未指定: 审查项目中所有符合条件的文件

### Step 3: 执行审查

按阶段顺序执行检查，每个阶段：
1. 读取规范文件对应章节
2. 扫描项目文件匹配检查项
3. 记录检查结果（通过/警告/违规/阻断）

### Step 4: 生成报告

必须输出：
1. 文件报告 → `docs/code-review/{timestamp}-code-review.md`
2. 控制台摘要 → Box Drawing 格式汇总

---

## 规则文件结构

### 后端规则（rules/backend/）

| 文件 | 阶段 | 规范依据 |
|------|------|----------|
| `phase1-architecture.md` | Phase 1: 架构与包结构 | `01-architecture.md` |
| `phase2-database.md` | Phase 2: 数据库设计 | `03-database.md` |
| `phase3-coding.md` | Phase 3: 代码规范 | `02-coding-standards.md` |
| `phase4-business.md` | Phase 4: 业务逻辑与配置 | `04-exception-logging.md`、`06-config-env.md`、`10-transaction.md` |
| `phase5-security.md` | Phase 5: 安全规范 | `11-security.md` |
| `phase6-testing.md` | Phase 6: 测试与可观测 | `09-unit-test.md`、`05-api-docs.md`、`08-observability.md`、`07-infra-runtime.md` |

### 前端规则（rules/frontend/）

| 文件 | 阶段 | 规范依据 |
|------|------|----------|
| `phase1-architecture.md` | Phase 1: 前端架构与目录结构 | `13-front-arch.md §2-3` |
| `phase2-component.md` | Phase 2: 组件与样式规范 | `13-front-arch.md §5`、`14-ui-ux.md §8-13` |
| `phase3-coding.md` | Phase 3: 编码规范 | `13-front-arch.md §4` |
| `phase4-state.md` | Phase 4: 状态管理与API | `13-front-arch.md §6-8` |
| `phase5-security.md` | Phase 5: 安全与性能 | `13-front-arch.md §13-14` |
| `phase6-testing.md` | Phase 6: 测试与文档 | `13-front-arch.md §11-12、15` |

---

## 统一输出格式

### 文件输出

```
docs/code-review/{yyyy-MM-dd-HHmmss}-code-review.md
```

### 控制台输出（统一模板）

```
╔══════════════════════════════════════════════════════════════════════╗
║                     {target} 代码审查报告                            ║
╠══════════════════════════════════════════════════════════════════════╣
║  审查时间: {timestamp}                                               ║
║  审查范围: {target} - Phase {phases}                                 ║
║  审查模块: {module}                                                   ║
║  审查文件: {file}                                                     ║
╠══════════════════════════════════════════════════════════════════════╣
║                        审查结果汇总                                   ║
╠══════════════════════════════════════════════════════════════════════╣
║  Phase 1: 架构与结构     ✅ {n}   ⚠️ {n}   ❌ {n}   🚫 {n}          ║
║  Phase 2: 数据/组件       ✅ {n}   ⚠️ {n}   ❌ {n}   🚫 {n}          ║
║  Phase 3: 代码规范        ✅ {n}   ⚠️ {n}   ❌ {n}   🚫 {n}          ║
║  Phase 4: 业务/状态       ✅ {n}   ⚠️ {n}   ❌ {n}   🚫 {n}          ║
║  Phase 5: 安全规范        ✅ {n}   ⚠️ {n}   ❌ {n}   🚫 {n}          ║
║  Phase 6: 测试与可观测    ✅ {n}   ⚠️ {n}   ❌ {n}   🚫 {n}          ║
╠══════════════════════════════════════════════════════════════════════╣
║  总计                    ✅ {n}   ⚠️ {n}   ❌ {n}   🚫 {n}          ║
╚══════════════════════════════════════════════════════════════════════╝

🚫 阻断项（必须立即修复）:
   1. [文件名:行号] {问题描述}
   2. ...

❌ 违规项:
   1. [文件名:行号] {问题描述}
   2. ...

⚠️ 高优先级项:
   1. [文件名:行号] {问题描述}
   2. ...

📋 中优先级项:
   1. [文件名:行号] {问题描述}
   2. ...

📝 低优先级项:
   1. [文件名:行号] {问题描述}
   2. ...

📁 完整报告已保存至: docs/code-review/{timestamp}-code-review.md
```

---

## 状态说明

| 状态 | 含义 | 优先级 |
|------|------|--------|
| ✅ 通过 | 符合规范 | - |
| ⚠️ 警告 | 存在潜在问题，建议优化 | 中 |
| ❌ 违规 | 违反规范，需要修复 | 高 |
| 🚫 阻断 | 严重问题，必须立即修复 | 阻断 |

---

## 执行示例

```bash
# 全栈审查
/kn-code-review

# 仅前端审查
/kn-code-review --target frontend

# 仅后端 Phase 3
/kn-code-review --target backend --phase 3

# 定点审查指定文件
/kn-code-review --file knowledge-front/src/pages/admin/TemplateManagement/index.tsx

# 严格模式（含推荐项）
/kn-code-review --strict
```