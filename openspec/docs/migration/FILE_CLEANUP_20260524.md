# 文件清理记录

> **日期**: 2026-05-24
> **操作**: 统一管理测试日志、脚本和编译产物

---

## 清理范围

将散落在 `examples/` 目录的临时文件统一归类到 `openspec/tests/` 管理。

---

## 文件移动清单

### 1. 日志文件 → `openspec/tests/logs/`

| 原路径 | 新路径 | 说明 |
|--------|--------|------|
| `examples/compile_error.log` | `openspec/tests/logs/compile_error.log` | 编译错误日志 |
| `examples/compile_full.log` | `openspec/tests/logs/compile_full.log` | 完整编译日志 |
| `examples/startup.log` | `openspec/tests/logs/startup.log` | 应用启动日志 |
| `examples/test_results.log` | `openspec/tests/logs/test_results.log` | 测试结果日志 |

### 2. 脚本文件 → `openspec/tests/scripts/`

| 原路径 | 新路径 | 说明 |
|--------|--------|------|
| `examples/run-regression-tests.sh` | `openspec/tests/scripts/run-regression-tests.sh` | 回归测试脚本 |

### 3. 编译产物 → `openspec/tests/artifacts/`

| 原路径 | 新路径 | 说明 |
|--------|--------|------|
| `examples/compile_errors.txt` | `openspec/tests/artifacts/compile_errors.txt` | 编译错误摘要 |
| `examples/test_compile.txt` | `openspec/tests/artifacts/test_compile.txt` | 测试编译输出 |

---

## 配置更新

### .gitignore 更新

在 `examples/.gitignore` 中添加：

```gitignore
### 临时日志和编译产物 ###
*.log
*.txt
!README.md
!CLAUDE.md
!**/src/main/**/*.txt
!**/src/test/**/*.txt

### 环境配置 ###
.env
.env.local
.env.*.local
```

**规则说明**:
- 项目根目录和 `examples/` 下的 `*.log`, `*.txt` 将被 Git 忽略
- `openspec/tests/` 下的日志文件需要手动管理
- 源代码中的文档文件不受影响

---

## 目录结构变化

### 新增目录

```
openspec/tests/
├── logs/          # 测试执行日志
├── scripts/       # 测试执行脚本
└── artifacts/     # 编译产物和临时文件
```

### 新增文档

| 文档 | 路径 | 说明 |
|------|------|------|
| TEST_LOGS_INDEX.md | openspec/tests/ | 测试日志索引 |

---

## 使用指南

### 运行测试脚本

```bash
# 赋予执行权限
chmod +x openspec/tests/scripts/run-regression-tests.sh

# 执行回归测试
./openspec/tests/scripts/run-regression-tests.sh
```

### 查看测试日志

```bash
# 查看编译错误
cat openspec/tests/artifacts/compile_errors.txt

# 查看测试结果
cat openspec/tests/logs/test_results.log

# 查看应用启动日志
cat openspec/tests/logs/startup.log
```

### 归档新的日志

```bash
# 移动新生成的日志到对应目录
mv new_test.log openspec/tests/logs/
mv compile_output.txt openspec/tests/artifacts/

# 更新索引
# 编辑 openspec/tests/TEST_LOGS_INDEX.md
```

---

## 后续维护

### 日志清理策略

| 文件类型 | 保留策略 | 清理时机 |
|---------|---------|---------|
| 编译日志 | 保留最新3个 | 每次成功编译后 |
| 测试日志 | 保留最新5个 | 每周清理 |
| 启动日志 | 保留最新1个 | 每次启动后覆盖 |
| 编译产物 | 按需保留 | 版本发布时归档 |

### 自动化脚本

可考虑添加自动化清理脚本：

```bash
#!/bin/bash
# cleanup_logs.sh

# 保留最新的3个编译日志
ls -t openspec/tests/logs/compile_*.log | tail -n +4 | xargs rm -f

# 保留最新的5个测试日志
ls -t openspec/tests/logs/test_*.log | tail -n +6 | xargs rm -f
```

---

## 总结

✅ **已完成**:
- 7个文件从 `examples/` 移动到 `openspec/tests/`
- 创建3个子目录用于分类管理
- 更新 `.gitignore` 配置
- 创建测试日志索引文档
- 更新 openspec INDEX.md

✅ **效果**:
- `examples/` 目录保持整洁
- 测试资源统一管理
- 未来临时文件自动忽略
- 文档结构更清晰

---

**操作人**: Claude Code
**审核**: 待审核
**版本**: v1.0.0
