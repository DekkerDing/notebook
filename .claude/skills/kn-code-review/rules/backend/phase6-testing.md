# Phase 6: 测试与可观测

> 规范依据: `openspec/conventions/09-unit-test.md`、`05-api-docs.md`、`08-observability.md`、`07-infra-runtime.md`

## 6.1 单元测试规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 测试方法命名使用 `should_预期结果_when_测试条件` 格式 | 09-unit-test.md §1.2 | 推荐 | ⚠️ |
| 测试使用 AAA 模式（Arrange/Act/Assert） | 09-unit-test.md §1.3 | 强制 | ❌ |
| 禁止在测试中使用 Thread.sleep | 09-unit-test.md §1.8 | 禁止 | 🚫 |
| 禁止测试方法包含 System.out.println | 09-unit-test.md §1.8 | 禁止 | 🚫 |
| Mock 外部依赖，不 Mock 被测类自身 | 09-unit-test.md §1.5 | 强制 | ❌ |

## 6.2 可观测性规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| traceId 注入 MDC（请求入口） | 08-observability.md §1.1 | 强制 | 🚫 |
| 请求结束清理 MDC（finally 中 MDC.clear()） | 08-observability.md §1.1 | 强制 | 🚫 |
| 外部调用传递 traceId | 08-observability.md §2.4 | 强制 | ❌ |
| 指标使用 MeterRegistry 预注册 | 08-observability.md §3.1 | 强制 | ❌ |
| 禁止在热路径中动态创建 Meter | 08-observability.md §9 | 禁止 | 🚫 |

## 6.3 基础设施规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 线程池使用 TTL 装饰（传递上下文） | 07-infra-runtime.md §3 | 强制 | 🚫 |
| 禁止 Executors.newFixedThreadPool 等隐藏 OOM 风险的创建方式 | 07-infra-runtime.md §2.1 | 禁止 | 🚫 |
| @Async 指定线程池名称（禁止无参 @Async） | 07-infra-runtime.md §2.3 | 强制 | 🚫 |
| 分布式锁使用 Redisson | 07-infra-runtime.md §8 | 强制 | 🚫 |
| 锁命名格式 `lock:<业务模块>:<资源标识>` | 07-infra-runtime.md §8.1 | 强制 | ❌ |
| 锁必须设置超时（禁止 lock.lock() 无超时） | 07-infra-runtime.md §8.4 | 禁止 | 🚫 |
| 缓存 Key 命名格式 `{project}:{module}:{resource}:{identifier}` | 07-infra-runtime.md §9.1 | 强制 | ❌ |
| 缓存必须设置 TTL（禁止永不过期） | 07-infra-runtime.md §9.2 | 强制 | 🚫 |
| 禁止在事务内直接删除/更新缓存 | 07-infra-runtime.md §9.4 | 禁止 | 🚫 |
| XXL-Job 任务必须实现幂等性 | 07-infra-runtime.md §1.3 | 强制 | 🚫 |
| XXL-Job 任务必须上报执行结果 | 07-infra-runtime.md §1.2 | 强制 | ❌ |
| ES 查询必须包含 tenant_id 过滤 | 07-infra-runtime.md §6 | 强制 | 🚫 |
| ES 批量写入使用 BulkRequest（禁止循环单条写入） | 07-infra-runtime.md §6 | 禁止 | 🚫 |
| SSE 连接注册到全局管理器（防止内存泄漏） | 07-infra-runtime.md §7.1 | 强制 | 🚫 |