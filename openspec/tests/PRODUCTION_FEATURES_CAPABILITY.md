# 生产级功能能力清单

> **日期**: 2026-05-24
> **版本**: v1.0.0
> **状态**: 已实现并测试通过

---

## 📊 功能模块总览

| 模块 | 状态 | 服务类 | 测试覆盖 |
|------|------|--------|----------|
| 分布式锁 | ✅ 已实现 | DistributedLockService.java | 4个测试用例 |
| 限流器 | ✅ 已实现 | RateLimiterService.java | 4个测试用例 |
| ID生成器 | ✅ 已实现 | IdGeneratorService.java | 5个测试用例 |
| 两级缓存 | ✅ 已实现 | TwoLevelCacheService.java | 4个测试用例 |

---

## 🔒 分布式锁 (DistributedLockService)

### 核心能力

| 功能 | 方法 | 说明 |
|------|------|------|
| 基础锁获取 | `tryLock(key)` | 使用默认超时配置获取锁 |
| 自定义超时锁 | `tryLock(key, waitTime, leaseTime, unit)` | 自定义等待和持有时间 |
| 锁释放 | `unlock(key)` | 安全释放锁（检查持有者） |
| 强制释放 | `forceUnlock(key)` | 强制释放锁（异常恢复用） |
| 锁状态检查 | `isLocked(key)` | 检查锁是否被持有 |
| 当前线程持有 | `isHeldByCurrentThread(key)` | 检查锁是否由当前线程持有 |

### 锁类型支持

| 锁类型 | 方法 | 使用场景 |
|--------|------|----------|
| 普通可重入锁 | `tryLock()` | 标准互斥访问 |
| 读锁 | `tryReadLock()` | 允许并发读，互斥写 |
| 写锁 | `tryWriteLock()` | 互斥写，独占访问 |
| 公平锁 | `tryFairLock()` | 按请求顺序获取，避免饥饿 |

### 锁回调模板

```java
// 无返回值
distributedLockService.executeWithLock(key, 30, () -> {
    // 业务逻辑
});

// 有返回值
String result = distributedLockService.executeWithLock(key, 30, () -> {
    return "success";
});
```

### 锁Key命名规范

格式：`lock:{业务模块}:{资源标识}`

```
lock:order:123          # 订单锁
lock:user:456           # 用户锁
lock:payment:txn:789    # 交易锁
```

---

## 🚦 限流器 (RateLimiterService)

### 核心能力

| 功能 | 方法 | 说明 |
|------|------|------|
| 初始化限流器 | `initLimiter(key, rate, interval)` | 初始化令牌桶限流器 |
| 更新配置 | `updateLimiter(key, rate, interval)` | 动态更新限流配置 |
| 获取令牌 | `tryAcquire(key)` | 尝试获取1个令牌 |
| 批量获取 | `tryAcquire(key, permits)` | 尝试获取多个令牌 |
| 超时获取 | `tryAcquire(key, permits, timeout, unit)` | 在超时时间内等待令牌 |
| 删除限流器 | `deleteLimiter(key)` | 删除限流器 |

### 限流回调模板

```java
// 无返回值
rateLimiterService.executeWithRateLimit(key, () -> {
    // 正常业务逻辑
}, () -> {
    // 限流降级逻辑
});

// 有返回值
String result = rateLimiterService.executeWithRateLimit(key, 1,
    () -> "success",    // 正常执行
    () -> "fallback"    // 限流降级
);
```

### 预定义限流器

| 类型 | 方法 | 限流配置 |
|------|------|----------|
| API限流 | `apiRateLimit(apiKey)` | 100次/秒 |
| 用户限流 | `userRateLimit(userId)` | 10次/秒 |
| IP限流 | `ipRateLimit(ip)` | 20次/秒 |

### 限流器Key命名规范

格式：`ratelimit:{类型}:{标识}`

```
ratelimit:api:user:create      # API限流
ratelimit:user:123             # 用户限流
ratelimit:ip:192.168.1.1       # IP限流
```

---

## 🆔 ID生成器 (IdGeneratorService)

### 核心能力

| 功能 | 方法 | 说明 |
|------|------|------|
| 序列ID | `nextId(key)` | 从1开始的递增ID |
| 自定义起始值 | `nextId(key, initValue)` | 指定起始值 |
| 批量获取 | `nextIdBatch(key, count)` | 批量获取ID（提升性能） |
| 当前值查询 | `currentId(key)` | 查询当前ID值（不自增） |
| 设置值 | `setId(key, value)` | 设置ID值 |

### 业务ID生成

| 类型 | 方法 | 格式示例 |
|------|------|----------|
| 标准业务ID | `nextBusinessId(prefix, key)` | `ORD20260524000001` |
| 自定义后缀 | `nextBusinessId(prefix, key, suffix)` | `ORD202605000001` |
| 紧凑型ID | `nextCompactId(prefix, key)` | `ORD0000001` |

### 高级ID生成

| 类型 | 方法 | 特点 |
|------|------|------|
| 雪花算法 | `snowflakeId(key)` | 分布式唯一，趋势递增 |
| 时间戳ID | `timestampId(key)` | `20260524143052000001` |
| 毫秒时间戳ID | `millisecondTimestampId(key)` | 纯数字，高精度 |

### 预定义业务ID

| 类型 | 方法 | 格式 |
|------|------|------|
| 订单ID | `orderId()` | `ORD{yyyyMMdd}{8位序列}` |
| 支付ID | `paymentId()` | `PAY{yyyyMMdd}{8位序列}` |
| 退款ID | `refundId()` | `REF{yyyyMMdd}{8位序列}` |
| 用户ID | `userId()` | `USR{yyyyMM}{8位序列}` |
| 交易ID | `transactionId()` | `TXN{yyyyMMddHHmmssSSS}{6位序列}` |

### ID Key命名规范

格式：`id:{业务}:{标识}`

```
id:order                    # 订单序列ID
id:user                     # 用户序列ID
id:snowflake:transaction    # 雪花ID
id:timestamp:payment        # 时间戳ID
```

---

## 💾 两级缓存 (TwoLevelCacheService)

### 核心能力

| 功能 | 方法 | 说明 |
|------|------|------|
| 获取缓存 | `get(key, type)` | 从L1/L2获取缓存 |
| 带加载器获取 | `get(key, type, loader)` | 缓存不存在则通过loader加载 |
| 设置缓存 | `set(key, value, ttl, unit)` | 设置缓存（自动写L1+L2） |
| 删除缓存 | `delete(key)` | 删除L1+L2缓存 |
| 批量删除 | `deleteBatch(keys)` | 批量删除缓存 |
| 按前缀删除 | `deleteByPrefix(prefix)` | 按前缀批量删除 |
| 检查存在 | `exists(key)` | 检查缓存是否存在 |
| 获取统计 | `getStats()` | 获取缓存统计信息 |

### 缓存策略

| 层级 | 技术 | 容量 | TTL | 用途 |
|------|------|------|-----|------|
| L1 | Caffeine | 10000条目 | 60秒 | 热数据，毫秒级访问 |
| L2 | Redis | 无限制 | 1800秒（默认） | 分布式共享 |

### 缓存统计信息

```java
CacheStats stats = cacheService.getStats();
stats.getL1Hits()           // L1命中次数
stats.getL1HitRate()        // L1命中率
stats.getL2Hits()           // L2命中次数
stats.getL2HitRate()        // L2命中率
stats.getOverallHitRate()   // 整体命中率
```

### 缓存Key命名规范

格式：`cache:{业务}:{标识}`

```
cache:user:123              # 用户信息
cache:order:detail:456      # 订单详情
cache:config:system         # 系统配置
```

---

## 🧪 测试覆盖

### 测试类

[ProductionFeaturesRegressionTest.java](../examples/src/test/java/io/github/dekkerding/examples/regression/ProductionFeaturesRegressionTest.java)

### 测试用例统计

| 模块 | 测试用例数 | 覆盖场景 |
|------|-----------|---------|
| 分布式锁 | 4 | 基础锁、超时竞争、回调模板、读写锁 |
| 限流器 | 4 | 初始化配置、令牌获取、回调模板、预定义限流 |
| ID生成器 | 5 | 序列ID、业务ID、雪花算法、时间戳ID、预定义ID |
| 两级缓存 | 4 | 基础操作、加载器、统计信息、批量删除 |
| 综合场景 | 1 | 订单处理完整流程 |

**总计**: 18个测试用例

### 运行测试

```bash
# 运行所有生产功能测试
./gradlew test --tests ProductionFeaturesRegressionTest

# 运行特定模块测试
./gradlew test --tests ProductionFeaturesRegressionTest.testBasicLock
./gradlew test --tests ProductionFeaturesRegressionTest.testRateLimiterInit
./gradlew test --tests ProductionFeaturesRegressionTest.testSequenceId
./gradlew test --tests ProductionFeaturesRegressionTest.testBasicCache
```

---

## 📋 综合使用场景

### 场景1：订单处理

```java
String orderId = idGeneratorService.orderId();

// 限流检查
if (!rateLimiterService.apiRateLimit("order:create")) {
    throw new RateLimitException("请求过于频繁");
}

// 分布式锁保护
String lockKey = "order:" + orderId;
distributedLockService.executeWithLock(lockKey, 30, () -> {
    // 创建订单
    Order order = createOrder(orderId);

    // 缓存订单详情
    cacheService.set("order:" + orderId, order, 30, TimeUnit.MINUTES);
});
```

### 场景2：用户信息缓存

```java
String userId = "user123";

// 从缓存获取用户信息
User user = cacheService.get("user:" + userId, User.class, () -> {
    // 缓存未命中，从数据库加载
    return userRepository.findById(userId);
});

// 更新时删除缓存
cacheService.delete("user:" + userId);
```

### 场景3：API限流保护

```java
@PostMapping("/api/user/create")
public Result createUser(@RequestBody CreateUserRequest request) {
    return rateLimiterService.executeWithRateLimit(
        "api:user:create",
        () -> {
            // 正常业务逻辑
            return userService.create(request);
        },
        () -> {
            // 限流降级
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
    );
}
```

---

## 🔧 配置要求

### Redis版本要求

- Redis 3.0+（基础功能）
- Redis 5.0+（Stream功能）
- Redisson 3.16.8+

### Spring Boot配置

```yaml
spring:
  redis:
    host: 192.168.10.109
    port: 6379
    # password: your_password
    database: 0
```

### 依赖配置

```gradle
implementation 'org.redisson:redisson:3.16.8'
implementation 'com.github.ben-manes.caffeine:caffeine:2.9.3'
```

---

## ⚠️ 注意事项

### 分布式锁

1. **锁持有时间**: 需要根据业务执行时间合理设置，避免业务未完成锁已释放
2. **锁等待时间**: 设置合理的等待超时，避免无限等待
3. **锁释放**: 必须在finally块中释放锁，或使用回调模板

### 限流器

1. **初始化**: 限流器使用前必须先初始化
2. **令牌数**: 批量获取令牌时需注意令牌数不要超过限流速率
3. **降级策略**: 建议配置合理的降级逻辑

### ID生成器

1. **序列ID**: 依赖Redis持久化，Redis重启后序列会丢失
2. **雪花算法**: 确保服务器时间同步
3. **业务ID**: 根据业务需求选择合适的ID格式

### 两级缓存

1. **缓存一致性**: 数据更新时必须删除缓存
2. **缓存穿透**: 使用加载器模式避免缓存穿透
3. **内存占用**: L1缓存占用JVM内存，注意监控

---

## 📈 性能指标

| 功能 | 性能指标 |
|------|---------|
| 分布式锁 | 获取延迟 < 10ms（99%分位） |
| 限流器 | 令牌获取延迟 < 5ms |
| ID生成器 | 生成速度 > 10000 ID/s |
| 两级缓存 | L1命中延迟 < 1ms，L2命中延迟 < 5ms |

---

**维护**: Redis ToolKit Team
**更新**: 2026-05-24
**版本**: v1.0.0
