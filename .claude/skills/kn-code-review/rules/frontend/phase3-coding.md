# Phase 3: 编码规范

> 规范依据: `openspec/conventions/13-front-arch.md §4`

## 3.1 命名规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 禁止中文命名 | 13-front-arch.md §4.1 | 强制 | 🚫 |
| 变量使用 camelCase | 13-front-arch.md §4.1 | 强制 | 🚫 |
| 常量使用 UPPER_SNAKE_CASE | 13-front-arch.md §4.1 | 强制 | ❌ |
| 函数使用 camelCase 动词开头 | 13-front-arch.md §4.1 | 强制 | ❌ |
| 组件文件使用 PascalCase | 13-front-arch.md §4.1 | 强制 | 🚫 |
| 工具文件使用 camelCase | 13-front-arch.md §4.1 | 强制 | ❌ |
| 测试文件使用 .test.tsx 后缀 | 13-front-arch.md §4.1 | 强制 | ❌ |

## 3.2 导入规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 导入顺序正确（React → 第三方 → 内部 → 样式） | 13-front-arch.md §4.2 | 推荐 | ⚠️ |
| 使用 @/ 绝对路径导入 | 13-front-arch.md §4.2 | 强制 | 🚫 |
| 禁止相对路径深度导入（../../../../） | 13-front-arch.md §4.2 | 强制 | 🚫 |

## 3.3 类型规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 禁止使用 any | 13-front-arch.md §4.4 | 强制 | 🚫 |
| 使用 interface 定义类型 | 13-front-arch.md §4.4 | 推荐 | ⚠️ |
| 使用 type 联合类型替代枚举 | 13-front-arch.md §4.4 | 推荐 | ⚠️ |
| 函数显式声明参数和返回值类型 | 13-front-arch.md §4.5 | 强制 | 🚫 |
| API 类型在 api/{module}/types.ts 定义 | 13-front-arch.md §11.1 | 强制 | ❌ |
| 页面类型在 pages/{page}/types.ts 定义 | 13-front-arch.md §11.1 | 强制 | ❌ |
| 组件 Props 就近定义 | 13-front-arch.md §11.1 | 推荐 | ⚠️ |

## 3.4 注释规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 注释解释"为什么"而非"是什么" | 13-front-arch.md §4.3 | 强制 | ⚠️ |
| 删除被注释的代码 | 13-front-arch.md §4.3 | 强制 | 🚫 |
| 删除无意义注释 | 13-front-arch.md §4.3 | 强制 | ⚠️ |

## 3.5 组件代码结构审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 组件结构有序（类型定义 → 实现 → Hooks → 回调 → 渲染 → 导出） | 13-front-arch.md §4.6 | 推荐 | ⚠️ |
| 使用 useCallback/useMemo 优化性能 | 13-front-arch.md §13.1 | 推荐 | ⚠️ |
| 复杂逻辑抽离为 Custom Hook | 13-front-arch.md §3.2 | 推荐 | ⚠️ |
| 回调函数引用稳定（useCallback） | 13-front-arch.md §4.6 | 推荐 | ⚠️ |