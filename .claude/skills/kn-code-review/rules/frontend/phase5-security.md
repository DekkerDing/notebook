# Phase 5: 安全与性能规范

> 规范依据: `openspec/conventions/13-front-arch.md §13-14`

## 5.1 XSS 防护审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 禁止使用 dangerouslySetInnerHTML | 13-front-arch.md §14.1 | 强制 | 🚫 |
| 用户输入内容需转义 | 13-front-arch.md §14.1 | 强制 | 🚫 |
| 富文本使用 DOMPurify 净化 HTML | 13-front-arch.md §14.1 | 强制 | 🚫 |

## 5.2 敏感信息处理审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 敏感信息存储在环境变量 | 13-front-arch.md §14.2 | 强制 | 🚫 |
| Token 不写入日志 | 13-front-arch.md §14.2 | 强制 | 🚫 |
| 日志输出需脱敏处理 | 13-front-arch.md §14.2 | 强制 | ❌ |

## 5.3 CSRF 防护审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 请求头携带 CSRF Token | 13-front-arch.md §14.3 | 推荐 | ⚠️ |
| Cookie 设置 SameSite 属性 | 13-front-arch.md §14.3 | 推荐 | ⚠️ |

## 5.4 性能优化审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 长列表使用虚拟列表（>100项） | 13-front-arch.md §13.1 | 推荐 | ⚠️ |
| 复杂计算结果使用 useMemo 缓存 | 13-front-arch.md §13.1 | 推荐 | ⚠️ |
| 使用 React.memo 减少不必要渲染 | 13-front-arch.md §13.1 | 推荐 | ⚠️ |
| 页面/组件使用 lazy + Suspense 代码分割 | 13-front-arch.md §13.1 | 推荐 | ⚠️ |
| 第三方库分离（vendor chunk） | 13-front-arch.md §13.2 | 推荐 | ⚠️ |
| 按需加载（动态 import） | 13-front-arch.md §13.2 | 推荐 | ⚠️ |