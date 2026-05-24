# Redis Stream Kafka功能模块 - 部署检查清单

## 部署前准备

### 1. Redis集群状态验证

```bash
# 检查Redis集群节点状态
# 节点1: 192.168.10.107:6379-6381
# 节点2: 192.168.10.109:6379-6381

# 使用telnet测试连接
telnet 192.168.10.107 6379
telnet 192.168.10.107 6380
telnet 192.168.10.107 6381
telnet 192.168.10.109 6379
telnet 192.168.10.109 6380
telnet 192.168.10.109 6381

# 如果有redis-cli，检查集群状态
redis-cli -c -h 192.168.10.107 -p 6379 -a drk@2025 cluster nodes
```

### 2. 环境变量配置

确保 `.env` 文件已配置（参考 `.env.example`）:

```bash
cd F:\workspace\notebook\examples
cp .env.example .env
# 编辑 .env 填入实际值
```

关键配置项：
```bash
REDIS_CLUSTER_NODES=192.168.10.107:6379,192.168.10.107:6380,192.168.10.107:6381,192.168.10.109:6379,192.168.10.109:6380,192.168.10.109:6381
REDIS_PASSWORD=drk@2025
REDIS_STREAM_ENABLED=true
REDIS_PARTITION_ENABLED=false  # 根据需要启用分区
```

---

## 当前代码状态

### ✅ 已修复的问题

| 问题 | 状态 | 说明 |
|------|------|------|
| 编译错误 | ✅ | 0 errors |
| Spring Bean冲突 | ✅ | EventSerializer添加@Primary |
| PartitionManager硬编码 | ✅ | 动态生成分区列表 |
| 测试断言错误 | ✅ | PT-006, PT-011已修复 |

### ⚠️ 测试状态

- **分区回归测试**: 11/11 ✅
- **完整测试套件**: 88/108 (81%)
- **失败原因**: Redis连接不可达

---

## 部署步骤

### 步骤1: 验证Redis连接

```bash
# 方式1: 使用Gradle内置测试
cd F:\workspace\notebook\examples
./gradlew test --tests "*RedisConnectionTest*" --console=plain

# 方式2: 使用应用健康检查
./gradlew bootRun
# 访问: http://localhost:8080/actuator/health
```

### 步骤2: 运行完整测试

```bash
# 清理并重新测试
cd F:\workspace\notebook\examples
./gradlew clean test --console=plain

# 查看测试报告
# 打开: build/reports/tests/test/index.html
```

### 步骤3: 启动应用

```bash
# 开发模式
./gradlew bootRun

# 生产模式
./gradlew clean build
java -jar build/libs/examples-0.0.1-SNAPSHOT.jar
```

### 步骤4: 验证功能

```bash
# 检查健康状态
curl http://localhost:8080/actuator/health

# 检查指标
curl http://localhost:8080/actuator/metrics

# 发布测试事件
curl -X POST http://localhost:8080/api/events/test \
  -H "Content-Type: application/json" \
  -d '{"type":"OrderCreated","data":{"orderId":"test-123"}}'
```

---

## 功能验证清单

### 核心功能

- [ ] **Stream创建**: 消费者组自动创建
- [ ] **消息发布**: 事件成功写入Stream
- [ ] **消息消费**: XREADGROUP正常消费
- [ ] **幂等性**: 重复消息被正确过滤
- [ ] **重试机制**: 失败消息自动重试
- [ ] **DLQ**: 死信队列正常工作

### 分区功能（可选）

- [ ] **分区创建**: 按配置数量创建分区Stream
- [ ] **哈希路由**: 相同Key路由到相同分区
- [ ] **轮询路由**: 消息均匀分布到各分区
- [ ] **动态切换**: 分区策略支持热切换

### 性能指标

- [ ] **吞吐量**: >10,000 TPS (小消息)
- [ ] **延迟**: P99 <100ms (端到端)
- [ ] **并发**: 支持多消费者负载均衡

---

## 常见问题排查

### 问题1: Redis连接失败

**错误信息**:
```
io.lettuce.core.RedisConnectionException: Unable to connect to 192.168.10.107:6379
```

**解决方案**:
1. 检查Redis服务是否运行
2. 检查防火墙规则
3. 验证密码配置
4. 确认网络连通性

### 问题2: 消费者组已存在

**错误信息**:
```
BUSYGROUP Consumer Group name already used
```

**解决方案**: 这是正常情况，代码会自动处理

### 问题3: 测试数据残留

**解决方案**:
```bash
# 清理Redis测试数据
redis-cli -c -h 192.168.10.107 -p 6379 -a drk@2025 FLUSHDB
```

---

## 配置建议

### 生产环境配置

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
        max-wait: 1000ms
    stream:
      event:
        poll-timeout: 2000
        batch-size: 10
        partition:
          enabled: true
          count: 3
          strategy: hash
        idempotent:
          enabled: true
          key-ttl: 86400
        retry:
          max-attempts: 3
          initial-interval: 1000
          backoff-multiplier: 2.0
        dlq:
          enabled: true
```

### JVM参数建议

```bash
java -jar examples.jar \
  -Xms2g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+DisableExplicitGC \
  -Dspring.redis.stream.event.partition.enabled=true
```

---

## 监控指标

### 关键指标

| 指标 | 目标值 | 监控方式 |
|------|--------|---------|
| 消息吞吐量 | >10K TPS | Actuator metrics |
| 消费延迟 | <100ms P99 | Actuator metrics |
| 错误率 | <0.1% | 日志统计 |
| Redis连接数 | 稳定 | Redis INFO |
| 消息堆积 | <1000 | XINFO GROUPS |

---

## 文档参考

- **Task.md**: 完整问题修复记录
- **TEST_PLAN.md**: 测试计划
- **TEST_EXECUTION_GUIDE.md**: 测试执行指南
- **OPTIMIZATION_GUIDE.md**: 性能优化指南
- **MIGRATION_SUMMARY.md**: 迁移总结

---

**维护团队**: 开发+测试团队
**更新时间**: 2026-05-23
**下次审查**: 生产部署后1周
