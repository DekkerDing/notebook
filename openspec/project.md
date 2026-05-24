# Redis Toolkit 项目规范

> **版本**: v1.0.0  
> **更新日期**: 2026 年 5 月 23 日  
> **详细规范目录**: `openspec/conventions/`

---

## 项目全局定义

| 项目 | 值 |
|------|-----|
| 项目名称 | redis-toolkit |
| 项目类型 | Java工具库/Spring Boot Starter |
| 基础包路径 | `io.github.dekkerding.redis` |
| 模块名称格式 | `redis-toolkit-{模块}` |

---

## 后端项目全局定义

| 项目 | 值 |
|------|-----|
| 基础包路径 | `io.github.dekkerding.redis` |
| 后端模块名称格式 | `redis-toolkit-{模块}` |

---

## 模块定义

| 模块 | 说明 | 状态 |
|------|------|------|
| `redis-toolkit-common` | 公共模块：注解、异常、模型、序列化、工具 | ✅ |
| `redis-toolkit-core` | 核心功能：Stream、锁、缓存、限流等 | ✅ |
| `redis-toolkit-extension` | 扩展功能：监控、性能、追踪 | ✅ |
| `redis-toolkit-api` | API模块：自动配置、启动引导 | ✅ |

---

## 后端详细规范索引

### 架构规约

| 规范文件 | 规范内容概述 |
|----------|-------------|
| [01-architecture.md](conventions/01-architecture.md) | 工具库分层架构、模块划分、设计原则、设计模式 |

### 研发规约

| 规范文件 | 规范内容概述 |
|----------|-------------|
| [02-coding-standards.md](conventions/02-coding-standards.md) | 包命名、类命名、接口设计、实现类模板、配置类模板 |
| [04-exception-logging.md](conventions/04-exception-logging.md) | 异常体系、错误码格式、日志级别、日志内容 |
| [05-api-docs.md](conventions/05-api-docs.md) | 公共API文档规范、JavaDoc要求 |

### 配置与运行时规约

| 规范文件 | 规范内容概述 |
|----------|-------------|
| [06-config-env.md](conventions/06-config-env.md) | 配置文件结构、配置属性、自动配置、环境管理 |
| [07-infra-runtime.md](conventions/07-infra-runtime.md) | Redis连接管理、线程池、缓存使用、分布式锁 |
| [08-observability.md](conventions/08-observability.md) | 监控指标、健康检查、性能数据、日志规范 |

### 测试规约

| 规范文件 | 规范内容概述 |
|----------|-------------|
| [09-unit-test.md](conventions/09-unit-test.md) | 单元测试、集成测试(Testcontainers)、性能测试 |

### 安全规约

| 规范文件 | 规范内容概述 |
|----------|-------------|
| [11-security.md](conventions/11-security.md) | Redis连接安全、Key访问控制、数据安全、命令执行安全 |

---

## 技术栈

### 核心框架

| 技术 | 版本 | 说明 |
|------|------|------|
| **JDK** | 8+ | 兼容性优先 |
| **Spring Boot** | 2.6+ | 自动配置支持 |
| **Lombok** | 1.18.30+ | 精简代码 |

### Redis相关

| 技术 | 版本 | 说明 |
|------|------|------|
| **Spring Data Redis** | 2.6+ | Redis操作基础 |
| **Lettuce** | 6.2+ | 异步/响应式客户端 |
| **Jedis** | 4.3+ | 备选客户端 |

### 工具库

| 依赖 | 版本 | 用途 |
|------|------|------|
| Guava | 30.0+ | 集合工具、缓存 |
| Hutool | 5.8+ | 通用工具 |
| Commons Lang3 | 3.12+ | 字符串工具 |
| Caffeine | 3.0+ | 本地缓存 |
| Jackson | 2.15+ | JSON序列化 |

### 监控与测试

| 技术 | 版本 | 说明 |
|------|------|------|
| **Micrometer** | 1.9+ | 指标采集 |
| **Actuator** | 2.6+ | 健康检查 |
| **JUnit 5** | 5.8+ | 单元测试 |
| **Testcontainers** | 1.17+ | 集成测试 |

---

## 功能模块规划

### 已实现模块

| 模块 | 功能 | 文档 |
|------|------|------|
| **redis-stream** | Redis替代Kafka | 01-architecture.md |

### 计划中模块

| 模块 | 功能 | 优先级 |
|------|------|--------|
| **redis-lock** | 分布式锁 | 高 |
| **redis-cache** | 两级缓存 | 高 |
| **redis-rate-limit** | 限流 | 中 |
| **redis-id** | ID生成 | 中 |
| **redis-delay-queue** | 延时队列 | 中 |
| **redis-geo** | 地理服务 | 低 |
| **redis-hyperloglog** | 基数统计 | 低 |
| **redis-bloomfilter** | 布隆过滤器 | 低 |
| **redis-script** | Lua脚本 | 低 |

---

## 开发流程

### 1. 新增功能模块

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
# RedisToolkitAutoConfiguration.java

# 6. 编写测试
# <功能名>Test.java
```

### 2. 测试流程

```bash
# 单元测试
./gradlew test --tests <功能名>Test

# 集成测试
./gradlew test --tests <功能名>IntegrationTest

# 性能测试
./gradlew test --tests <功能名>PerformanceTest
```

---

## 性能基准

| 功能 | 目标 | 当前状态 |
|------|------|----------|
| Stream发布 | >10K TPS | ✅ 已达标 |
| Stream消费 | >10K TPS | ✅ 已达标 |
| 分布式锁 | <10ms P99 | ⏳ 待测试 |
| 缓存GET | <1ms P99 | ⏳ 待测试 |
| 限流判断 | <1ms P99 | ⏳ 待测试 |

---

## 规范复用

### 复制到其他工具库项目

这套规范可以快速复制到其他工具库项目（如定时任务工具集）：

```bash
# 1. 复制规范目录
cp -r openspec/conventions ../<新项目>/openspec/conventions

# 2. 全局替换
# redis → <新项目标识>
# stream → job/schedule等

# 3. 调整模块划分
# 根据<新项目>特点调整核心功能模块

# 4. 更新技术栈
# 替换为<新项目>相关技术
```

---

**文档维护**: Redis Toolkit Team  
**更新频率**: 每次规范变更后  
**下次审查**: 季度规范审查