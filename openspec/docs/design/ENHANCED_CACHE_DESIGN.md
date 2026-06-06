# 增强型两级缓存服务功能文档

> **版本**: v1.0.0  
> **更新时间**: 2026-06-06  
> **作者**: Redis ToolKit Team

---

## 概述

增强型两级缓存服务（`EnhancedTwoLevelCacheService`）是对标准两级缓存服务的全面升级，专注于**高可用性**、**降级策略**和**异常兜底**机制。

### 核心设计理念

1. **降级优先**: 系统异常时自动降级，保证基本功能可用
2. **异常兜底**: 所有异常都有兜底处理，不影响业务流程
3. **自动恢复**: 故障恢复后自动切换回正常模式
4. **监控完善**: 提供详细的统计和监控信息

---

## 核心特性

### 1. 自动降级策略

#### 降级触发条件

| 触发条件 | 降级行为 | 恢复条件 |
|----------|----------|----------|
| Redis连接失败 | 自动切换到L1模式 | Redis健康检查通过 |
| Redis操作超时 | 跳过L2操作 | 下次操作成功 |
| 手动触发降级 | 完全降级到L1 | 手动恢复 |

#### 降级模式

```java
// 自动降级示例
cacheService.get("user:123", User.class);  
// Redis不可用时，自动从L1获取，不会抛出异常

// 手动降级
cacheService.triggerDegradation("系统维护中");

// 检查降级状态
boolean isDegraded = cacheService.isDegraded();
String reason = cacheService.getDegradationReason();
long duration = cacheService.getDegradationDuration();

// 手动恢复
cacheService.recoverFromDegradation();
```

### 2. 异常兜底机制

#### 全局异常处理

所有缓存操作都包含异常兜底：

```java
// 获取缓存 - 异常时返回null
User user = cacheService.get("user:123", User.class);
// 不会抛出异常，即使Redis连接失败

// 带兜底值的获取
User user = cacheService.getWithFallback(
    "user:123", 
    User.class, 
    () -> userRepository.findById(123),  // 加载器
    new User("default", "Default User")   // 兜底值
);
// 加载器失败时返回兜底值
```

#### 分级异常处理

| 异常类型 | 处理策略 | 记录指标 |
|----------|----------|----------|
| L1缓存异常 | 返回null，不影响L2 | `l1Errors` |
| L2缓存异常 | 返回null或降级到L1 | `l2Errors` |
| 加载器异常 | 返回兜底值 | `loaderErrors` |
| 其他异常 | 返回null，记录日志 | `errors` |

### 3. 能力扩展

#### 批量操作

```java
// 批量获取
Map<String, User> users = cacheService.getBatch(
    Arrays.asList("user:1", "user:2", "user:3"), 
    User.class
);

// 批量设置
Map<String, User> userMap = new HashMap<>();
userMap.put("user:1", user1);
userMap.put("user:2", user2);
cacheService.setBatch(userMap, 30, TimeUnit.MINUTES);
```

#### 条件缓存

```java
// 仅当条件满足时缓存
cacheService.setIf(
    "user:123", 
    user, 
    30, TimeUnit.MINUTES, 
    user.isActive()  // 条件
);

// 仅当值非空时缓存
cacheService.setIfPresent("user:123", user, 30, TimeUnit.MINUTES);
```

#### 缓存预热

```java
// 批量预热
Map<String, User> warmUpData = loadHotData();
cacheService.warmUp(warmUpData, 30, TimeUnit.MINUTES);
```

### 4. 监控统计

#### 统计指标

```java
EnhancedCacheStats stats = cacheService.getStats();

// 基础指标
long l1Hits = stats.getL1Hits();           // L1命中次数
long l1Misses = stats.getL1Misses();       // L1未命中次数
long l2Hits = stats.getL2Hits();           // L2命中次数
long l2Misses = stats.getL2Misses();       // L2未命中次数

// 操作指标
long puts = stats.getPuts();               // 设置次数
long deletes = stats.getDeletes();         // 删除次数

// 错误指标
long errors = stats.getErrors();           // 总错误次数
long l1Errors = stats.getL1Errors();       // L1错误次数
long l2Errors = stats.getL2Errors();       // L2错误次数
long loaderErrors = stats.getLoaderErrors(); // 加载器错误次数

// 降级指标
long degradationCount = stats.getDegradationCount(); // 降级次数

// 命中率
double l1HitRate = stats.getL1HitRate();   // L1命中率
double l2HitRate = stats.getL2HitRate();   // L2命中率
```

#### 监控输出示例

```
EnhancedCacheStats{
  l1Hits=1000, l1Misses=100, l1HitRate=90.91%,
  l2Hits=80, l2Misses=20, l2HitRate=80.00%,
  puts=500, deletes=50,
  errors=5, l1Errors=2, l2Errors=3, loaderErrors=1,
  degradationCount=2
}
```

---

## 使用示例

### 基础使用

```java
@Autowired
private EnhancedTwoLevelCacheService cacheService;

// 获取用户
User user = cacheService.get("user:123", User.class);

// 带加载器获取
User user = cacheService.get("user:123", User.class, () -> {
    return userRepository.findById(123);
});

// 带兜底值获取
User user = cacheService.getWithFallback(
    "user:123", 
    User.class,
    () -> userRepository.findById(123),
    new User("default", "Default User")
);

// 设置缓存
cacheService.set("user:123", user, 30, TimeUnit.MINUTES);

// 删除缓存
cacheService.delete("user:123");
```

### 高级使用

```java
// 批量操作
List<String> keys = Arrays.asList("user:1", "user:2", "user:3");
Map<String, User> users = cacheService.getBatch(keys, User.class);

// 条件缓存
cacheService.setIf("user:123", user, 30, TimeUnit.MINUTES, user.isActive());

// 缓存预热
Map<String, User> hotData = loadHotUsers();
cacheService.warmUp(hotData, 30, TimeUnit.MINUTES);

// 监控降级状态
if (cacheService.isDegraded()) {
    log.warn("缓存服务降级中: reason={}", cacheService.getDegradationReason());
}
```

---

## 降级策略详解

### 降级层级

```
┌─────────────────────────────────────────────┐
│         正常模式（双层级）                   │
│  L1(本地) + L2(Redis) + 数据源             │
└─────────────────────────────────────────────┘
                    ↓ Redis故障
┌─────────────────────────────────────────────┐
│         降级模式（L1优先）                  │
│  L1(本地) + 数据源（L2跳过）                │
└─────────────────────────────────────────────┘
                    ↓ L1也不可用
┌─────────────────────────────────────────────┐
│         兜底模式（直接访问数据源）            │
│  数据源（所有缓存跳过）                      │
└─────────────────────────────────────────────┘
```

### 降级决策流程

```
开始获取缓存
    ↓
检查L1缓存
    ↓ 命中
返回数据
    ↓ 未命中
检查Redis健康状态
    ↓ 健康
从L2获取
    ↓ 成功
返回数据 + 回写L1
    ↓ 失败
记录L2错误 + 返回null
    ↓ 不健康/降级
跳过L2 + 返回null
    ↓ 数据为null
调用加载器
    ↓ 成功
返回数据 + 写入L1
    ↓ 失败
返回兜底值/null
```

---

## 性能优化

### L1缓存优化

1. **容量控制**: 最大10000条目，超过后自动淘汰
2. **TTL策略**: 默认1分钟，防止数据过时
3. **定时清理**: 每30秒清理过期条目
4. **并发友好**: 使用ConcurrentHashMap保证线程安全

### L2缓存优化

1. **健康检查**: 每30秒检查Redis连接状态
2. **自动跳过**: Redis不健康时自动跳过L2操作
3. **连接复用**: 使用Redisson连接池
4. **序列化优化**: 使用Jackson ObjectMapper

### 降级响应优化

1. **快速失败**: 操作失败时立即返回，不阻塞
2. **兜底值**: 预设兜底值，减少回源压力
3. **限流保护**: L1缓存满时自动淘汰旧数据
4. **监控告警**: 记录所有降级事件

---

## 监控告警建议

### 关键指标

| 指标 | 告警阈值 | 建议 |
|------|----------|------|
| 降级时长 | > 5分钟 | 检查Redis状态 |
| L1命中率 | < 50% | 增加L1容量或TTL |
| L2错误率 | > 10% | 检查Redis连接 |
| 加载器错误率 | > 5% | 检查数据源状态 |

### 告警策略

```java
// 示例：监控降级状态
@Scheduled(fixedRate = 60000)
public void monitorCacheDegradation() {
    if (cacheService.isDegraded()) {
        long duration = cacheService.getDegradationDuration();
        if (duration > 300000) { // 5分钟
            alertService.sendAlert("缓存降级超过5分钟: " + 
                cacheService.getDegradationReason());
        }
    }
}
```

---

## 配置建议

### 生产环境配置

```yaml
# application.yaml
cache:
  l1:
    max-size: 10000        # L1最大容量
    default-ttl: 60000      # L1默认TTL（毫秒）
    cleanup-interval: 30    # 清理间隔（秒）
  
  l2:
    default-ttl: 1800       # L2默认TTL（秒）
    health-check-interval: 30  # 健康检查间隔（秒）
  
  degradation:
    auto-recover: true     # 自动恢复
    alert-threshold: 300000 # 告警阈值（毫秒）
```

---

## 测试覆盖

增强型缓存服务包含全面的测试用例：

| 测试类 | 测试数 | 覆盖范围 |
|--------|-------|----------|
| EnhancedCacheFeaturesRegressionTest | 12 | 基础操作、降级策略、批量操作、监控统计 |

### 测试清单

- ✅ CACHE-E-001: 基础缓存操作
- ✅ CACHE-E-002: 带加载器的缓存获取
- ✅ CACHE-E-003: 带兜底值的缓存获取
- ✅ CACHE-E-010: 手动降级测试
- ✅ CACHE-E-020: 批量获取缓存
- ✅ CACHE-E-021: 批量设置缓存
- ✅ CACHE-E-030: 条件缓存测试
- ✅ CACHE-E-031: 值存在性缓存测试
- ✅ CACHE-E-040: 缓存预热测试
- ✅ CACHE-E-050: 缓存统计测试
- ✅ CACHE-E-060: 并发缓存操作测试
- ✅ CACHE-E-070: 异常序列化处理测试
- ✅ CACHE-E-080: 批量删除和前缀删除测试
- ✅ CACHE-E-090: 缓存清空和大小测试

---

## 文件清单

### 新增文件

1. `EnhancedTwoLevelCacheService.java` - 增强型缓存服务实现
2. `EnhancedCacheFeaturesRegressionTest.java` - 增强型缓存测试用例
3. `EnhancedCacheConfiguration.java` - 增强型缓存配置类

### 修改文件

- 无（增强型缓存服务是独立的新服务）

---

## 与标准缓存服务的对比

| 特性 | 标准缓存服务 | 增强型缓存服务 |
|------|--------------|----------------|
| 基础缓存操作 | ✅ | ✅ |
| 带加载器的缓存 | ✅ | ✅ |
| 批量操作 | ❌ | ✅ |
| 条件缓存 | ❌ | ✅ |
| 缓存预热 | ❌ | ✅ |
| 自动降级 | ❌ | ✅ |
| 异常兜底 | 部分 | 完整 |
| 降级监控 | ❌ | ✅ |
| 健康检查 | ❌ | ✅ |
| 详细统计 | 基础 | 增强 |

---

**维护**: Redis ToolKit Team  
**更新**: 2026-06-06  
**版本**: v1.0.0
