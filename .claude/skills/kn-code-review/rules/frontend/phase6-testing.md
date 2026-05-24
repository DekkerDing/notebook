# Phase 6: 测试与文档规范

> 规范依据: `openspec/conventions/13-front-arch.md §11-12、15`

## 6.1 TypeScript 类型审查

**自动检查**: 执行 `npm run typecheck`（即 `tsc --noEmit`），退出码 0 即为通过。

**执行命令**:
```bash
cd knowledge-front && npm run typecheck
```

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 禁止 any 类型 | 13-front-arch.md §12.1 | 强制 | 🚫 |
| Hooks 规则正确（react-hooks/rules-of-hooks） | 13-front-arch.md §12.1 | 强制 | 🚫 |
| 优先使用 const | 13-front-arch.md §12.1 | 强制 | ❌ |

## 6.2 ESLint/Prettier 审查

**自动检查**:
- ESLint: 执行 `npm run lint`（即 `eslint .`），退出码 0 即为通过
- Prettier: 执行 `npx prettier --check "**/*.{ts,tsx}"`，退出码 0 即为通过（注意 `npm run format` 是 `--write`，会直接修改文件，不适合审查场景）

**执行命令**:
```bash
cd knowledge-front && npm run lint
cd knowledge-front && npx prettier --check "**/*.{ts,tsx}"
```

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| printWidth 100 | 13-front-arch.md §12.2 | 强制 | ❌ |
| tabWidth 2 | 13-front-arch.md §12.2 | 强制 | ❌ |
| semi true | 13-front-arch.md §12.2 | 强制 | ❌ |
| singleQuote true | 13-front-arch.md §12.2 | 强制 | ❌ |
| trailingComma es5 | 13-front-arch.md §12.2 | 强制 | ❌ |
| commit 格式 type(scope): subject | 13-front-arch.md §12.3 | 强制 | ❌ |

## 6.3 国际化规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 语言包按模块划分命名空间 | 13-front-arch.md §15.2 | 推荐 | ⚠️ |
| 语言包文件位于 locales/ 目录 | 13-front-arch.md §15.1 | 强制 | 🚫 |

## 6.4 Git 分支与 Commit 规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 分支命名符合规范（feature/bugfix/refactor/*） | 13-front-arch.md §16.1 | 强制 | ❌ |
| Commit 格式正确（type(scope): subject） | 13-front-arch.md §16.2 | 强制 | ❌ |
| Commit header 最大 72 字符 | 13-front-arch.md §16.2 | 强制 | ❌ |