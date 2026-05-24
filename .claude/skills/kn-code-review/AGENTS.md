# kn-code-review 执行逻辑

## 执行步骤

### 1. 解析用户意图

根据用户命令确定审查目标：

| 用户输入 | 审查目标 |
|----------|----------|
| `/kn-code-review` 无参数 | 默认审查 `knowledge-front/src/` 下所有 `.tsx`/`.ts` 文件 |
| `/kn-code-review --target frontend` | 审查前端所有文件 |
| `/kn-code-review --target backend` | 审查后端所有文件 |
| `/kn-code-review --dir <path>` | 审查指定目录下的所有文件 |
| `/kn-code-review --file <path>` | 仅审查指定文件 |
| `/kn-code-review --diff` | 仅审查 Git 变更的文件 |

### 2. 获取待审查文件列表

```bash
# 默认：扫描前端目录
find knowledge-front/src -name "*.tsx" -o -name "*.ts" | grep -v "\.d\.ts" | grep -v "node_modules"

# 指定目录
find <dir> -name "*.tsx" -o -name "*.ts"

# Git 变更
git diff --name-only HEAD~1 -- '*.tsx' '*.ts'

# 指定文件
echo "<file-path>"
```

### 3. 加载规范文件

读取项目规范：
- `openspec/conventions/13-front-arch.md`（前端架构）
- `openspec/conventions/14-ui-ux.md`（UI/UX 设计）
- `openspec/conventions/01-architecture.md`（后端架构）
- `openspec/conventions/03-database.md`（数据库设计）
- 等其他相关规范

### 4. 执行审查

按 Phase 顺序（1-6）对每个文件执行检查：
1. 读取对应规则文件
2. 逐条检查规范项
3. 记录检查结果（通过/警告/违规/阻断）

### 5. 生成报告

输出到：
- 控制台：Box Drawing 格式摘要
- 文件：`docs/code-review/{timestamp}-code-review.md`