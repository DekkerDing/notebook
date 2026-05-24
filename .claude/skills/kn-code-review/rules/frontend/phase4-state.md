# Phase 4: 状态管理与 API 规范

> 规范依据: `openspec/conventions/13-front-arch.md §6-8`

## 4.1 Zustand 使用审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| Store 按业务域拆分 | 13-front-arch.md §6.1 | 强制 | 🚫 |
| 避免单个 store 过大 | 13-front-arch.md §6.1 | 推荐 | ⚠️ |
| 共享状态在公共 store，私有状态在组件内部 | 13-front-arch.md §6.1 | 强制 | ❌ |
| 复杂状态更新使用 immer 中间件 | 13-front-arch.md §6.1 | 推荐 | ⚠️ |
| Token 存储在 localStorage | 13-front-arch.md §6.2 | 强制 | 🚫 |
| 表单草稿存储在 sessionStorage | 13-front-arch.md §6.2 | 推荐 | ⚠️ |
| UI 状态保存在内存 | 13-front-arch.md §6.2 | 推荐 | ⚠️ |

## 4.2 API 层规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| API 模块包含 types/request/index 三文件 | 13-front-arch.md §7.1 | 强制 | 🚫 |
| API 方法显式声明返回类型（Promise<T>） | 13-front-arch.md §7.4.1 | 强制 | 🚫 |
| 请求封装统一处理 Token/错误 | 13-front-arch.md §7.2 | 强制 | 🚫 |
| 401 跳转登录页 | 13-front-arch.md §7.3 | 强制 | 🚫 |
| 403 提示无权限 | 13-front-arch.md §7.3 | 强制 | 🚫 |
| 500 提示服务器错误 | 13-front-arch.md §7.3 | 强制 | ❌ |
| 网络错误提示网络异常 | 13-front-arch.md §7.3 | 强制 | ❌ |

## 4.3 useRequest 使用审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 查询类请求不使用 manual | 13-front-arch.md §7.4.2 | 强制 | 🚫 |
| 变更类（POST/PUT/DELETE）必须 manual: true | 13-front-arch.md §7.4.2 | 强制 | 🚫 |
| mutation 成功后调用 refresh() 刷新列表 | 13-front-arch.md §7.4.3 | 推荐 | ⚠️ |

## 4.4 路由规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 路由路径全小写、下划线连接 | 13-front-arch.md §8.1 | 强制 | 🚫 |
| 页面组件使用懒加载（lazy + Suspense） | 13-front-arch.md §8.1 | 强制 | 🚫 |
| 路由配置集中在 routes/index.tsx | 13-front-arch.md §8.1 | 强制 | ❌ |
| 多级路由在页面目录下创建 routes.tsx | 13-front-arch.md §8.1 | 推荐 | ⚠️ |
| 需鉴权的路由使用 ProtectedRoute | 13-front-arch.md §8.2 | 强制 | 🚫 |