# Redis Stream Kafka功能模块 - 回归测试执行指南

## 测试概览

本项目包含完整的回归测试套件，覆盖功能、性能、稳定性等多个维度。

### 测试套件结构

```
src/test/java/io/github/dekkerding/examples/regression/
├── PartitionRegressionTest.java       # 分区功能回归测试
├── IdempotentRegressionTest.java      # 幂等性功能回归测试
├── PerformanceRegressionTest.java     # 性能回归测试
└── StabilityRegressionTest.java       # 稳定性回归测试
```

---

## 快速开始

### 1. 环境准备

确保Redis集群可访问：
```bash
# 检查Redis连接
redis-cli -c -h 192.168.10.109 -p 6379 -a drk@2025 ping
redis-cli -c -h 192.168.10.107 -p 6379 -a drk@2025 ping
```

### 2. 运行所有回归测试

```bash
cd examples
./gradlew test --tests *.regression.* -i
```

### 3. 运行特定测试套件

```bash
# 分区功能测试
./gradlew test --tests PartitionRegressionTest

# 幂等性测试
./gradlew test --tests IdempotentRegressionTest

# 性能测试
./gradlew test --tests PerformanceRegressionTest
```

### 4. 运行特定测试用例

```bash
# 运行单个测试
./gradlew test --tests "PartitionRegressionTest.testHashPartitionConsistency"

# 运行特定分组的测试
./gradlew test --tests "PartitionRegressionTest.HashPartitionStrategyTests"
```

---

## 测试用例说明

### 分区功能测试 (PartitionRegressionTest)

| 测试ID | 测试名称 | 验证点 | 预期结果 |
|-------|---------|--------|---------|
| PT-001 | 哈希分区一致性 | 相同Key路由 | 相同分区 |
| PT-002 | 哈希分区分布 | 不同Key分布 | 相对均匀 |
| PT-003 | 轮询分区顺序 | 依次分配 | 0→1→2循环 |
| PT-004 | 单分区边界 | partition=1 | 只有一个分区 |
| PT-007 | 分区Stream创建 | 分区功能启用 | 所有分区存在 |
| PT-009 | 跨分区消息顺序 | 相同Key的消息 | 保持顺序 |
| PT-011 | 轮询分区并发 | 10线程并发 | 无消息丢失 |
| PT-012 | 分区消费者负载均衡 | 3消费者×3分区 | 负载均衡 |

### 幂等性测试 (IdempotentRegressionTest)

| 测试ID | 测试名称 | 验证点 | 预期结果 |
|-------|---------|--------|---------|
| IT-001 | 基础幂等性 | 重复消息 | 只处理1次 |
| IT-002 | 幂等性禁用 | 重复消息 | 都被处理 |
| IT-003 | 分布式幂等性 | 10个消费者 | 只处理1次 |
| IT-005 | 消息ID过期 | TTL=2秒 | 过期后可重新处理 |
| IT-006 | 不同消费者组 | 2个不同组 | 各处理1次 |
| IT-007 | 清理已处理记录 | cleanup调用 | 记录被删除 |
| IT-008 | 批量幂等性检查 | 批量场景 | 正确返回状态 |
| IT-009 | 并发幂等性检查 | 100个线程 | 无竞态条件 |
| IT-010 | 特殊字符消息ID | Unicode等 | 正常处理 |

### 性能测试 (PerformanceRegressionTest)

| 测试ID | 测试名称 | 目标指标 | 测试条件 |
|-------|---------|---------|---------|
| TP-001 | 小消息吞吐量 | >10000 TPS | 1KB消息 |
| TP-002 | 中消息吞吐量 | >500 TPS | 10KB消息 |
| TP-004 | 分区吞吐量 | >2000 TPS | 3分区 |
| LT-001 | 端到端延迟 | P99<500ms | 1000条消息 |
| LT-002 | 发布延迟 | P99<10ms | 1000次发布 |
| CC-001 | 并发发布 | >5000 TPS | 100线程 |
| CC-004 | 并发幂等性 | 只处理1次 | 50消费者 |
| RS-001 | 内存占用 | <500MB | 10000条消息 |
| RS-002 | CPU使用率 | <80% | 5000条消息 |

---

## 测试执行策略

### 冒烟测试 (5分钟)

快速验证核心功能：
```bash
./gradlew test --tests "*SmokeTest*"
```

### 功能回归测试 (15分钟)

验证所有功能点：
```bash
./gradlew test --tests "PartitionRegressionTest" \
               --tests "IdempotentRegressionTest"
```

### 性能回归测试 (10分钟)

验证性能指标：
```bash
./gradlew test --tests "PerformanceRegressionTest"
```

### 完整回归测试 (30分钟)

运行所有测试：
```bash
./gradlew test --tests *.regression.*
```

---

## 测试报告

### 查看测试报告

测试完成后，报告生成在：
```
examples/build/reports/tests/test/index.html
```

### 关键指标

- **测试通过率**: 目标 >95%
- **代码覆盖率**: 目标 >85%
- **性能指标**: 见上表
- **缺陷密度**: 目标 <2/KLOC

---

## CI/CD集成

### Jenkins Pipeline示例

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }
        stage('Unit Tests') {
            steps {
                sh './gradlew test --tests "*Test"'
            }
        }
        stage('Regression Tests') {
            steps {
                sh './gradlew test --tests *.regression.*'
            }
        }
        stage('Performance Tests') {
            steps {
                sh './gradlew test --tests *Performance*'
            }
        }
    }
    post {
        always {
            junit '**/build/test-results/**/*.xml'
        }
    }
}
```

### GitHub Actions示例

```yaml
name: Regression Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '8'
          distribution: 'temurin'

      - name: Run regression tests
        run: ./gradlew test --tests *.regression.*

      - name: Publish test report
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Regression Tests
          path: '**/build/test-results/**/*.xml'
```

---

## 故障排查

### 常见问题

**Q: 测试连接Redis失败**
```
A: 检查Redis配置和密码
redis-cli -c -h 192.168.10.109 -p 6379 -a drk@2025 ping
```

**Q: 性能测试超时**
```
A: 系统资源可能不足，检查CPU和内存使用情况
top -p $(pgrep -f gradle)
```

**Q: 并发测试失败**
```
A: 检查线程池大小和系统最大文件描述符
ulimit -n
```

### 调试建议

1. **启用DEBUG日志**
```bash
./gradlew test --info --tests PartitionRegressionTest
```

2. **单独运行失败的测试**
```bash
./gradlew test --tests "PartitionRegressionTest.testHashPartitionConsistency" --info
```

3. **查看详细堆栈**
```bash
./gradlew test --stacktrace
```

---

## 持续改进

### 测试覆盖率目标

| 模块 | 当前覆盖率 | 目标覆盖率 |
|------|-----------|-----------|
| 分区功能 | 95% | >95% |
| 幂等性 | 92% | >95% |
| 消费者组 | 88% | >90% |
| 性能测试 | 80% | >85% |

### 后续增强

- [ ] 添加混沌工程测试
- [ ] 集成JMeter进行压力测试
- [ ] 添加性能基准对比
- [ ] 实现测试数据自动化生成

---

**文档维护**: 测试团队
**更新频率**: 每个迭代
**联系方式**: test-team@example.com
