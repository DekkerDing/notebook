# 测试日志索引

> **版本**: v1.0.0
> **更新日期**: 2026-05-24
> **说明**: 统一管理项目测试相关的日志、脚本和编译产物

---

## 目录结构

```
openspec/tests/
├── logs/                    # 测试执行日志
│   ├── compile_error.log    # 编译错误日志
│   ├── compile_full.log     # 完整编译日志
│   ├── startup.log          # 应用启动日志
│   └── test_results.log     # 测试结果日志
│
├── scripts/                 # 测试执行脚本
│   └── run-regression-tests.sh  # 回归测试执行脚本
│
└── artifacts/               # 编译产物和临时文件
    ├── compile_errors.txt   # 编译错误摘要
    └── test_compile.txt     # 测试编译输出
```

---

## 日志文件说明

### 1. 编译日志

| 文件 | 说明 | 生成时间 | 用途 |
|------|------|---------|------|
| `compile_error.log` | 编译错误日志 | 2026-05-24 | 编译失败时的详细错误信息 |
| `compile_full.log` | 完整编译日志 | 2026-05-24 | Gradle编译完整输出，含警告 |
| `artifacts/compile_errors.txt` | 编译错误摘要 | 2026-05-24 | 提取的编译错误信息 |

**查看编译错误**:
```bash
# 查看编译错误摘要
cat openspec/tests/artifacts/compile_errors.txt

# 查看完整编译日志
cat openspec/tests/logs/compile_full.log
```

### 2. 测试日志

| 文件 | 说明 | 生成时间 | 用途 |
|------|------|---------|------|
| `test_results.log` | 测试结果日志 | 2026-05-24 | 回归测试执行结果 |
| `artifacts/test_compile.txt` | 测试编译输出 | 2026-05-24 | 测试代码编译结果 |

**查看测试结果**:
```bash
# 查看测试结果
cat openspec/tests/logs/test_results.log

# 查看测试编译
cat openspec/tests/artifacts/test_compile.txt
```

### 3. 应用日志

| 文件 | 说明 | 生成时间 | 用途 |
|------|------|---------|------|
| `startup.log` | 应用启动日志 | 2026-05-24 | 应用启动完整日志，含JIT预热 |

**查看启动日志**:
```bash
# 查看应用启动日志
cat openspec/tests/logs/startup.log

# 查看JIT预热日志
grep "JIT" openspec/tests/logs/startup.log
```

---

## 测试脚本

### run-regression-tests.sh

回归测试执行脚本，用于运行完整的测试套件。

**使用方法**:
```bash
# 赋予执行权限
chmod +x openspec/tests/scripts/run-regression-tests.sh

# 执行回归测试
./openspec/tests/scripts/run-regression-tests.sh

# 或从项目根目录执行
cd examples
../openspec/tests/scripts/run-regression-tests.sh
```

**脚本功能**:
- 清理旧的测试结果
- 编译主代码和测试代码
- 运行单元测试
- 运行集成测试
- 生成测试报告

---

## 日志管理规范

### 1. 日志生成规则

根据 `.gitignore` 配置，以下文件将被忽略：

```gitignore
### 临时日志和编译产物 ###
*.log
*.txt
!README.md
!CLAUDE.md
!**/src/main/**/*.txt
!**/src/test/**/*.txt
```

**规则说明**:
- 项目根目录和 `examples/` 目录下的 `*.log` 和 `*.txt` 将被 Git 忽略
- `openspec/tests/` 目录下的日志文件需要手动管理
- 源代码中的文档文件不受影响

### 2. 日志归档流程

**新增日志时**:
```bash
# 1. 将日志移动到对应目录
mv compile_xxx.log openspec/tests/logs/
mv test_xxx.txt openspec/tests/artifacts/

# 2. 更新本索引文件
# 在对应章节添加日志说明

# 3. 提交到Git
git add openspec/tests/
git commit -m "docs: 添加测试日志"
```

### 3. 日志清理策略

| 文件类型 | 保留策略 | 清理时机 |
|---------|---------|---------|
| 编译错误日志 | 保留最新 | 每次编译成功后清理旧日志 |
| 测试结果日志 | 保留最新 | 每次测试后更新 |
| 启动日志 | 保留最新 | 每次启动后覆盖 |
| 编译产物 | 按需保留 | 版本发布时归档 |

---

## 快速查看命令

### 查看最近的编译错误

```bash
tail -50 openspec/tests/logs/compile_error.log
```

### 查看最近的测试结果

```bash
grep -E "(PASSED|FAILED|ERROR)" openspec/tests/logs/test_results.log
```

### 查看应用启动是否成功

```bash
grep -E "(Started|Error|Exception)" openspec/tests/logs/startup.log
```

### 统计测试通过率

```bash
grep -c "PASSED" openspec/tests/logs/test_results.log
grep -c "FAILED" openspec/tests/logs/test_results.log
```

---

## 相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| [测试执行指南](../TEST_EXECUTION_GUIDE.md) | `openspec/tests/` | 测试执行详细指南 |
| [测试计划](../TEST_PLAN.md) | `openspec/tests/` | 测试计划文档 |
| [性能测试报告](../PERFORMANCE_TEST_REPORT.md) | `openspec/tests/` | 性能测试结果 |

---

**文档维护**: Redis工具集团队
**最后更新**: 2026-05-24
**版本**: v1.0.0
