# Phase 4: 业务逻辑与配置

> 规范依据: `openspec/conventions/04-exception-logging.md`、`06-config-env.md`、`10-transaction.md`

## 4.1 异常处理规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 错误码实现 ErrorCode 接口 | 04-exception-logging.md §1.1 | 强制 | 🚫 |
| 错误码格式 `{system-name}-{type-code}-{seq}` | 04-exception-logging.md §1.2 | 强制 | ❌ |
| BusinessException 用于业务规则错误 | 04-exception-logging.md §1.1 | 强制 | ❌ |
| SystemException 用于系统异常 | 04-exception-logging.md §1.1 | 强制 | ❌ |
| Controller 层不捕获异常（交由 GlobalExceptionHandler） | 04-exception-logging.md §1.3 | 强制 | ❌ |
| 禁止 catch 后吞掉异常不抛出 | 04-exception-logging.md §1.5 | 禁止 | 🚫 |
| 禁止抛出 new RuntimeException("xxx") | 04-exception-logging.md §1.5 | 禁止 | 🚫 |

## 4.2 日志规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 使用 @Slf4j（禁止手动创建 Logger） | 04-exception-logging.md §2.1 | 强制 | 🚫 |
| 日志级别使用正确（ERROR/WARN/INFO/DEBUG） | 04-exception-logging.md §2.2 | 强制 | ❌ |
| 日志内容使用占位符 {}（禁止字符串拼接） | 04-exception-logging.md §2.4 | 强制 | 🚫 |
| 敏感信息脱敏（手机号/Token/邮箱等） | 04-exception-logging.md §2.6 | 强制 | 🚫 |
| 禁止在日志中打印密码/Token 原文 | 04-exception-logging.md §2.6 | 禁止 | 🚫 |
| 批量操作只在循环外打汇总日志 | 04-exception-logging.md §2.4 | 强制 | ⚠️ |
| 异常日志必须包含完整堆栈 | 04-exception-logging.md §2.4 | 强制 | ❌ |

## 4.3 事务规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| @Transactional 只加在 application 层（AppServiceImpl） | 10-transaction.md §1.1 | 强制 | 🚫 |
| 写操作声明 rollbackFor = Exception.class | 10-transaction.md §1.2 | 强制 | 🚫 |
| 查询方法声明 readOnly = true | 10-transaction.md §1.4 | 强制 | ❌ |
| 写操作设置合理 timeout | 10-transaction.md §1.5 | 推荐 | ⚠️ |
| 事务内禁止 RPC/HTTP 调用 | 10-transaction.md §1.6 | 禁止 | 🚫 |
| 事务内禁止发送 MQ 消息 | 10-transaction.md §1.7 | 禁止 | 🚫 |
| 批量操作分批提交（≤500 条/批） | 10-transaction.md §1.10 | 强制 | 🚫 |
| 禁止同类内 this 调用事务方法（自调用失效） | 10-transaction.md §1.8 | 禁止 | 🚫 |

## 4.4 配置规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 使用 @ConfigurationProperties + @Component 绑定配置 | 06-config-env.md §3 | 强制 | ❌ |
| 敏感配置使用 Jasypt 加密（ENC 格式） | 06-config-env.md §4 | 强制 | 🚫 |
| 配置文件使用 kebab-case 命名 | 06-config-env.md §2 | 强制 | ❌ |
| @Value 禁止散落在业务类中 | 06-config-env.md §9.3 | 禁止 | ⚠️ |
| Yaml 配置按模块分组 | 06-config-env.md §2 | 强制 | ⚠️ |

## 4.5 API 文档规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| Controller 使用 @Api/@ApiOperation 注解 | 05-api-docs.md §2 | 强制 | ⚠️ |
| DTO/VO 字段使用 @ApiModelProperty | 05-api-docs.md §3 | 强制 | ⚠️ |
| 多端隔离（Admin/Operator/Client Controller 分离） | 05-api-docs.md §1.1 | 强制 | ❌ |
| URL 前缀符合规范（/api/v1/admin/ 等） | 05-api-docs.md §1.1 | 强制 | ❌ |