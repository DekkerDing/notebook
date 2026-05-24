# Redis工具集项目 - AI协作规范

> **项目定位**: Java后端工具库 / Spring Boot Starter
> **技术栈**: Redis, Spring Boot, Lettuce, Redisson
> **更新日期**: 2026-05-24

---

## 1. 语言规则

- 与我交流时，始终使用简体中文
- 回答内容必须使用简体中文
- 思考过程、计划、分析、步骤说明、错误解释，也必须使用简体中文
- 不要切换到英文，除非：
  - 我明确要求英文
  - 代码、命令、API 字段名、报错原文必须保留英文
- 代码注释默认使用简体中文
- 生成的提交说明、变更总结、实现说明，默认使用简体中文

---

## 2. 代码修改规范预读规则

> **激活条件**：仅当项目根目录存在 `openspec/conventions/` 目录时，本条规则才生效。

在普通对话中修改代码（修复 Bug、新增功能、重构等）时，
须根据修改内容**按需读取**对应规范文件，确保产出与项目规范一致。

### 2.1 预读映射表

| 修改内容 | 须读取的规范文件 |
|----------|----------------|
| 新建/修改接口、实现类 | `02-coding-standards.md` |
| 新建/修改配置类 | `02-coding-standards.md` + `06-config-env.md` |
| 异常处理 / 错误码 | `04-exception-logging.md` |
| API文档 / JavaDoc注释 | `05-api-docs.md` |
| 配置属性 / 环境变量 | `06-config-env.md` |
| Redis连接 / 线程池 | `07-infra-runtime.md` |
| 缓存 / 分布式锁 | `07-infra-runtime.md` |
| 监控指标 / 日志 | `08-observability.md` |
| 测试用例编写 | `09-unit-test.md` + `17-prompts.md` |
| AI辅助开发 / 代码生成 | `17-prompts.md` |
| 安全相关（密码、加密） | `11-security.md` |
| 架构设计 / 模块划分 | `01-architecture.md` |
| 纯粹修改文案 / 变量名 | **无需读取（零开销）** |

### 2.2 执行规则

1. **仅读一次**：同一轮对话中，已读过的规范无需重复读取
2. **最小读取**：只读规范中相关章节，无需通读全文
3. **冲突优先**：若需求与项目规范冲突，优先遵守项目规范，并向用户说明冲突原因
4. **静默执行**：读规范时直接执行，无需向用户汇报完整内容
5. **规范路径**：`openspec/conventions/`

---

## 3. Redis工具集开发规则

### 3.1 项目架构认知

**四层架构**:
```
┌─────────────────────────────────────┐
│   API层 (自动配置、启动引导)         │
├─────────────────────────────────────┤
│   Extension层 (监控、性能、追踪)     │
├─────────────────────────────────────┤
│   Core层 (Stream、锁、缓存、限流等)  │
├─────────────────────────────────────┤
│   Common层 (注解、异常、序列化)      │
└─────────────────────────────────────┘
```

**模块划分**:
- `redis-toolkit-common`: 公共模块
- `redis-toolkit-core`: 核心功能
- `redis-toolkit-extension`: 扩展功能
- `redis-toolkit-api`: API模块

### 3.2 设计模式优先级

| 优先级 | 设计模式 | 使用场景 |
|--------|---------|---------|
| HIGH | 策略模式 | 分区策略、序列化策略 |
| HIGH | 工厂模式 | Publisher/Consumer创建 |
| HIGH | 模板方法 | Redis操作模板 |
| MEDIUM | 包装器 | 幂等、重试装饰 |
| MEDIUM | 观察者 | 事件监听 |

### 3.3 命名约定

**包命名**: `io.github.dekkerding.redis.{layer}.{feature}`
**接口命名**: `{功能名}` 或 `I{功能名}`
**实现类命名**: `Redis{功能名}` 或 `{功能名}Impl`
**配置类命名**: `{功能名}Properties` / `{功能名}AutoConfiguration`

**示例**:
```
io.github.dekkerding.redis.core.stream.publisher.EventPublisher
io.github.dekkerding.redis.core.stream.publisher.RedisStreamEventPublisher
io.github.dekkerding.redis.config.RedisStreamEventProperties
```

### 3.4 核心功能模块

| 模块 | 状态 | 说明 |
|------|------|------|
| redis-stream | ✅ | Redis替代Kafka |
| redis-lock | ⏳ | 分布式锁（计划中） |
| redis-cache | ⏳ | 两级缓存（计划中） |
| redis-rate-limit | ⏳ | 限流（计划中） |
| redis-id | ⏳ | ID生成（计划中） |

---

## 4. Redis连接信息

**开发/测试环境**:
- 主机: `192.168.10.109:6379`
- 备机: `192.168.10.107:6379`
- 用途: 集成测试、功能验证

**本地开发**:
- 主机: `localhost:6379`
- 密码: 无（本地环境）

---

## 5. 开发工作流

### 5.1 新增功能模块

```bash
# 1. 在core下创建新模块包
mkdir -p redis-toolkit-core/src/main/java/io/github/dekkerding/redis/core/<新功能>

# 2. 定义接口
# <功能名>.java

# 3. 实现Redis操作
# Redis<功能名>.java

# 4. 创建配置类
# <功能名>Properties.java

# 5. 注册自动配置
# <功能名>AutoConfiguration.java

# 6. 编写测试
# <功能名>Test.java
```

### 5.2 测试流程

```bash
# 单元测试
./gradlew test --tests <功能名>Test

# 集成测试（需要Redis连接）
./gradlew test --tests <功能名>IntegrationTest

# 性能测试
./gradlew test --tests <功能名>PerformanceTest
```

---

## 6. 常见命令

### 6.1 Redis操作

```bash
# 连接Redis
redis-cli -h 192.168.10.109 -p 6379

# 查看Stream信息
XINFO STREAM <stream-key>

# 查看消费者组
XINFO GROUPS <stream-key>

# 查看Pending消息
XPENDING <stream-key> <group-name> - + -
```

### 6.2 构建命令

```bash
# 清理构建
./gradlew clean

# 编译
./gradlew build

# 跳过测试构建
./gradlew build -x test
```

---

## 7. 重要提醒

1. **密码安全**: 禁止在代码中硬编码Redis密码，必须使用环境变量
2. **Key命名**: 使用应用前缀隔离，格式：`{应用}:{功能}:{资源标识}`
3. **异常处理**: 统一使用 `RedisException` 及其子类
4. **日志规范**: 使用结构化日志，包含traceId、操作类型、资源标识
5. **指标采集**: 核心操作必须记录指标（Counter/Timer）
6. **测试覆盖**: 核心功能行覆盖率 ≥ 80%

---

**文档维护**: Redis Toolkit Team
**项目URL**: https://github.io/dekkerding/redis-toolkit