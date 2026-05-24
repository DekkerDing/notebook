# Phase 5: 安全规范

> 规范依据: `openspec/conventions/11-security.md`

## 5.1 认证与会话安全审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 使用 @BusinessOperator 获取用户身份 | 11-security.md §1.1 | 强制 | 🚫 |
| 禁止 @RequestHeader 读取 X-User-Id | 11-security.md §1.1 | 禁止 | 🚫 |
| tenantId 从 SessionUserInfo 获取 | 11-security.md §2.1 | 强制 | 🚫 |
| 禁止从请求参数接收 tenantId | 11-security.md §2.1 | 禁止 | 🚫 |
| 资源操作校验归属（tenantId 匹配） | 11-security.md §2.2 | 强制 | 🚫 |

## 5.2 输入验证与注入防护审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 使用 @Valid 触发校验 | 11-security.md §3.1 | 强制 | 🚫 |
| 字符串字段设置 @Size 上限 | 11-security.md §3.1 | 强制 | ❌ |
| 禁止 Map<String, Object> 接收参数 | 11-security.md §3.1 | 禁止 | 🚫 |
| 预编译参数 #{}（禁止 ${}） | 11-security.md §3.2 | 强制 | 🚫 |
| 动态排序白名单校验 | 11-security.md §3.2 | 强制 | ❌ |
| 富文本 XSS 过滤 | 11-security.md §3.3 | 强制 | 🚫 |

## 5.3 敏感数据保护审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 敏感配置使用 Jasypt 加密 | 11-security.md §4.1 | 强制 | 🚫 |
| 日志禁止打印密码/Token/手机号原文 | 11-security.md §4.2 | 禁止 | 🚫 |
| 响应禁止返回密码 Hash | 11-security.md §4.3 | 禁止 | 🚫 |
| @JsonIgnore 过滤敏感字段 | 11-security.md §4.3 | 强制 | 🚫 |

## 5.4 API 接口安全审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 分页 pageSize 最大 100 | 11-security.md §5.2 | 强制 | 🚫 |
| 文件上传校验类型/大小/ Magic Bytes | 11-security.md §5.3 | 强制 | 🚫 |
| 文件名使用 UUID 重生成 | 11-security.md §5.3 | 强制 | ❌ |
| 禁止上传可执行文件 | 11-security.md §5.3 | 强制 | 🚫 |

## 5.5 多租户与数据隔离审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| MyBatis Plus 多租户插件启用 | 11-security.md §8.1 | 强制 | 🚫 |
| BlockAttackInnerInterceptor 启用 | 11-security.md §8.2 | 强制 | 🚫 |
| 批量删除校验 ID 列表 ≤500 | 11-security.md §8.2 | 强制 | ❌ |
| 系统表在 IGNORE_TABLES 中声明 | 03-database.md §12.2 | 强制 | ❌ |