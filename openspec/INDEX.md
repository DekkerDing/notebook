# OpenSpec 文档中心

> **Redis工具集项目** 规范、文档、任务记录索引

---

## 📋 快速导航

| 类别 | 路径 | 说明 |
|------|------|------|
| **规范文件** | [conventions/](conventions/) | 项目开发规范（10个） |
| **任务记录** | [tasks/](tasks/) | 开发任务和日志 |
| **测试文档** | [tests/](tests/) | 测试计划和报告 |
| **部署文档** | [docs/deployment/](docs/deployment/) | 部署相关 |
| **优化文档** | [docs/optimization/](docs/optimization/) | 性能优化 |
| **设计文档** | [docs/design/](docs/design/) | 技术设计 |
| **使用指南** | [docs/guide/](docs/guide/) | 使用说明 |
| **AI协作** | [ai-collaboration/](ai-collaboration/) | AI对话记录 |
| **生产功能** | 📋 见下方 | 生产级核心功能模块 |

---

## 📁 目录结构

```
openspec/
├── INDEX.md             # 📋 本文件（文档索引）
├── project.md           # 📊 项目规范总览
│
├── conventions/         # 📜 项目规范
│   ├── 01-architecture.md       # 架构规范
│   ├── 02-coding-standards.md  # 编码规范
│   ├── 04-exception-logging.md # 异常和日志
│   ├── 05-api-docs.md          # API文档
│   ├── 06-config-env.md        # 配置管理
│   ├── 07-infra-runtime.md     # 运行时规范
│   ├── 08-observability.md     # 可观测性
│   ├── 09-unit-test.md        # 测试规范
│   ├── 11-security.md          # 安全规范
│   └── 17-prompts.md           # AI指令集
│
├── docs/                # 📚 项目文档
│   ├── deployment/      # 部署文档
│   │   ├── DEPLOYMENT.md
│   │   └── DEPLOYMENT_CHECKLIST.md
│   ├── optimization/    # 优化文档
│   │   └── OPTIMIZATION_GUIDE.md
│   ├── migration/       # 迁移文档
│   │   └── MIGRATION_SUMMARY.md
│   ├── design/          # 设计文档
│   └── guide/           # 使用指南
│
├── tasks/               # 📝 任务记录
│   └── Task.md          # v6.0 性能优化版
│
├── tests/               # 🧪 测试文档
│   ├── TEST_EXECUTION_GUIDE.md
│   ├── TEST_PLAN.md
│   ├── TEST_REPORT.md
│   ├── PERFORMANCE_TEST_REPORT.md
│   ├── TEST_LOGS_INDEX.md
│   ├── DELAYED_QUEUE_TEST_SUMMARY.md
│   ├── logs/            # 测试执行日志
│   ├── scripts/         # 测试执行脚本
│   └── artifacts/       # 编译产物和临时文件
│
└── ai-collaboration/    # 🤖 AI协作
    └── AGENTS.md
```

---

## 📜 项目规范 (conventions/)

### 核心规范

| 文件 | 标题 | 主要内容 |
|------|------|---------|
| [01-architecture.md](conventions/01-architecture.md) | 架构规范 | 四层架构、模块划分、设计模式 |
| [02-coding-standards.md](conventions/02-coding-standards.md) | 编码规范 | 包命名、类命名、接口设计、实现模板 |
| [06-config-env.md](conventions/06-config-env.md) | 配置管理 | 配置结构、环境管理、敏感信息 |
| [08-observability.md](conventions/08-observability.md) | 可观测性 | 监控指标、健康检查、日志规范 |

### 开发规范

| 文件 | 标题 | 主要内容 |
|------|------|---------|
| [04-exception-logging.md](conventions/04-exception-logging.md) | 异常和日志 | 异常体系、错误码、日志规范 |
| [05-api-docs.md](conventions/05-api-docs.md) | API文档 | JavaDoc、接口文档、配置文档 |
| [07-infra-runtime.md](conventions/07-infra-runtime.md) | 运行时规范 | Redis连接、线程池、分布式锁 |
| [09-unit-test.md](conventions/09-unit-test.md) | 测试规范 | 单元测试、集成测试、Testcontainers |
| [11-security.md](conventions/11-security.md) | 安全规范 | 密码管理、加密、权限控制 |
| [17-prompts.md](conventions/17-prompts.md) | AI指令集 | Prompt模板、代码生成规则、AI协作规范 |

---

## 📚 项目文档 (docs/)

### 部署文档 (deployment/)

| 文件 | 说明 |
|------|------|
| [DEPLOYMENT.md](docs/deployment/DEPLOYMENT.md) | 完整部署指南 |
| [DEPLOYMENT_CHECKLIST.md](docs/deployment/DEPLOYMENT_CHECKLIST.md) | 部署检查清单 |

### 优化文档 (optimization/)

| 文件 | 说明 |
|------|------|
| [OPTIMIZATION_GUIDE.md](docs/optimization/OPTIMIZATION_GUIDE.md) | 性能优化指南 |

### 迁移文档 (migration/)

| 文件 | 说明 |
|------|------|
| [MIGRATION_SUMMARY.md](docs/migration/MIGRATION_SUMMARY.md) | 迁移摘要 |
| [FILE_CLEANUP_20260524.md](docs/migration/FILE_CLEANUP_20260524.md) | 文件清理记录 |
| [DOCS_CLEANUP_20260524.md](docs/migration/DOCS_CLEANUP_20260524.md) | 文档整理记录 |

### 设计文档 (design/)

| 文件 | 说明 |
|------|------|
| [CALL_CHAIN_ARCHITECTURE.md](docs/design/CALL_CHAIN_ARCHITECTURE.md) | 调用链架构图（含Mermaid图） |
| [DELAY_QUEUE_DESIGN.md](docs/design/DELAY_QUEUE_DESIGN.md) | 延迟队列设计文档 |

### 使用指南 (guide/)

| 文件 | 说明 |
|------|------|
| [USER_GUIDE.md](docs/guide/USER_GUIDE.md) | 用户使用指南 |
| [INTEGRATION_GUIDE.md](docs/guide/INTEGRATION_GUIDE.md) | 业务接入指南（含调用链图） |
| [DELAYED_QUEUE_GUIDE.md](docs/guide/DELAYED_QUEUE_GUIDE.md) | 延迟队列使用指南 |

---

## 📝 任务记录 (tasks/)

| 文件 | 版本 | 主要内容 |
|------|------|---------|
| [Task.md](tasks/Task.md) | v6.0 | 性能优化方案、Redisson功能、回归测试 |

---

## 🧪 测试文档 (tests/)

| 文件 | 说明 |
|------|------|
| [TEST_EXECUTION_GUIDE.md](tests/TEST_EXECUTION_GUIDE.md) | 测试执行指南 |
| [TEST_PLAN.md](tests/TEST_PLAN.md) | 测试计划 |
| [TEST_REPORT.md](tests/TEST_REPORT.md) | 测试报告 |
| [PERFORMANCE_TEST_REPORT.md](tests/PERFORMANCE_TEST_REPORT.md) | 性能测试报告 |
| [TEST_LOGS_INDEX.md](tests/TEST_LOGS_INDEX.md) | 测试日志索引 |
| [PRODUCTION_FEATURES_CAPABILITY.md](tests/PRODUCTION_FEATURES_CAPABILITY.md) | 生产功能能力清单 |

**测试资源目录**:
- `logs/` - 测试执行日志
- `scripts/` - 测试执行脚本
- `artifacts/` - 编译产物和临时文件

---

## 🚀 生产级核心功能

| 服务类 | 功能说明 | 状态 |
|--------|---------|------|
| [DistributedLockService.java](../examples/src/main/java/io/github/dekkerding/examples/application/DistributedLockService.java) | 分布式锁（同步锁、读写锁、公平锁、锁回调） | ✅ 已实现 |
| [RateLimiterService.java](../examples/src/main/java/io/github/dekkerding/examples/application/RateLimiterService.java) | 限流器（令牌桶、限流回调、预定义限流） | ✅ 已实现 |
| [IdGeneratorService.java](../examples/src/main/java/io/github/dekkerding/examples/application/IdGeneratorService.java) | ID生成器（序列ID、业务ID、雪花算法、时间戳ID） | ✅ 已实现 |
| [TwoLevelCacheService.java](../examples/src/main/java/io/github/dekkerding/examples/application/TwoLevelCacheService.java) | 两级缓存（L1/L2、缓存加载器、统计信息） | ✅ 已实现 |
| [ProductionFeaturesRegressionTest.java](../examples/src/test/java/io/github/dekkerding/examples/regression/ProductionFeaturesRegressionTest.java) | 生产功能回归测试套件 | ✅ 已实现 |

### 功能快速参考

**分布式锁**:
- `tryLock(key)` - 获取锁（默认配置）
- `tryLock(key, waitTime, leaseTime, unit)` - 获取锁（自定义超时）
- `unlock(key)` - 释放锁
- `executeWithLock(key, leaseTime, callback)` - 锁回调模板

**限流器**:
- `initLimiter(key, rate, interval)` - 初始化限流器
- `tryAcquire(key)` - 尝试获取1个令牌
- `executeWithRateLimit(key, callback, fallback)` - 限流回调模板

**ID生成器**:
- `nextId(key)` - 生成序列ID
- `nextBusinessId(prefix, key)` - 生成业务ID（前缀+日期+序列）
- `snowflakeId(key)` - 生成雪花算法ID
- `orderId()` / `paymentId()` - 预定义业务ID

**两级缓存**:
- `get(key, type)` - 获取缓存
- `set(key, value, ttl, unit)` - 设置缓存
- `delete(key)` - 删除缓存
- `getStats()` - 获取缓存统计信息

---

## 🤖 AI协作 (ai-collaboration/)

| 文件 | 说明 |
|------|------|
| [AGENTS.md](ai-collaboration/AGENTS.md) | AI协作规范、开发规则 |
| [CLAUDE_OPTIMIZATION.md](ai-collaboration/CLAUDE_OPTIMIZATION.md) | CLAUDE配置优化建议 |
| [FILE_STRUCTURE_ANALYSIS.md](ai-collaboration/FILE_STRUCTURE_ANALYSIS.md) | 文件结构分析报告 |

---

## 📄 文档管理规则

### 新增文档规则

根据 `CLAUDE.md` 配置，所有新生成的.md文档将自动归类：

| 文档类型 | 保存路径 | 示例 |
|---------|---------|------|
| 设计文档 | `openspec/docs/design/` | `Pipeline优化设计.md` |
| 部署文档 | `openspec/docs/deployment/` | `生产部署指南.md` |
| 优化文档 | `openspec/docs/optimization/` | `性能优化方案.md` |
| 使用指南 | `openspec/docs/guide/` | `快速开始.md` |
| 任务记录 | `openspec/tasks/` | `开发日志.md` |
| 测试文档 | `openspec/tests/` | `测试报告.md` |

### 测试资源管理规则

所有测试相关的日志、脚本和临时产物统一管理：

| 资源类型 | 保存路径 | 示例文件 |
|---------|---------|---------|
| 测试日志 | `openspec/tests/logs/` | `startup.log`, `test_results.log` |
| 测试脚本 | `openspec/tests/scripts/` | `run-regression-tests.sh` |
| 编译产物 | `openspec/tests/artifacts/` | `compile_errors.txt` |

**.gitignore 配置**:
- 项目根目录和 `examples/` 下的 `*.log`, `*.txt` 自动忽略
- `openspec/tests/` 下的日志文件需手动管理

### 根目录保留

仅以下文件保留在项目根目录：
- `README.md` - 项目说明（入口）
- `CLAUDE.md` - AI配置

---

## 🔍 文档搜索

### 按场景查找

| 需求 | 推荐文档 |
|------|---------|
| **开始新功能开发** | [02-coding-standards.md](conventions/02-coding-standards.md) + [01-architecture.md](conventions/01-architecture.md) |
| **添加配置项** | [06-config-env.md](conventions/06-config-env.md) |
| **编写测试用例** | [09-unit-test.md](conventions/09-unit-test.md) + [17-prompts.md](conventions/17-prompts.md) |
| **AI代码生成** | [17-prompts.md](conventions/17-prompts.md) |
| **添加监控指标** | [08-observability.md](conventions/08-observability.md) |
| **部署到生产** | [DEPLOYMENT.md](docs/deployment/DEPLOYMENT.md) + [DEPLOYMENT_CHECKLIST.md](docs/deployment/DEPLOYMENT_CHECKLIST.md) |
| **性能问题排查** | [OPTIMIZATION_GUIDE.md](docs/optimization/OPTIMIZATION_GUIDE.md) |
| **查看开发进度** | [Task.md](tasks/Task.md) |

---

**文档维护**: Redis工具集团队
**最后更新**: 2026-05-24
**版本**: v2.0
