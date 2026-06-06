# Redis中文字符支持配置文档

> **版本**: v1.0.0  
> **更新时间**: 2026-06-06  
> **作者**: Redis ToolKit Team

---

## 概述

本文档说明如何在Redis中正确存储和显示中文及其他Unicode字符，解决在Redis客户端中看到乱码的问题。

---

## 问题分析

### 乱码原因

在Redis客户端中看到乱码的原因通常是使用了**二进制序列化器**（如JDK默认序列化），而不是**字符串序列化器**。

| 序列化方式 | Redis客户端显示 | 中文支持 |
|------------|----------------|----------|
| 二进制序列化 | 乱码（如 `\xAC\xED\x00\x05t\x00\x05`） | ❌ |
| 字符串序列化 | 正常显示（如 `用户:123`） | ✅ |

---

## 解决方案

### 1. Redisson配置

**使用StringCodec作为默认编解码器**:

```java
@Configuration
public class RedissonConfiguration {

    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = new Config();

        // 使用StringCodec作为默认编解码器，支持中文
        Codec stringCodec = new StringCodec(StandardCharsets.UTF_8);

        config.useClusterServers()
                .setDefaultCodec(stringCodec)  // 设置默认编解码器
                .addNodeAddress(...)
                .setPassword("...");

        return Redisson.create(config);
    }
}
```

**配置说明**:
- `StringCodec`: 使用UTF-8编码的字符串编解码器
- `setDefaultCodec`: 设置为所有Redis操作使用StringCodec
- `StandardCharsets.UTF_8`: 确保使用UTF-8字符集

### 2. RedisTemplate配置

**使用StringRedisSerializer**:

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);

    // 使用StringRedisSerializer，支持中文
    StringRedisSerializer stringSerializer = 
        new StringRedisSerializer(StandardCharsets.UTF_8);

    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setValueSerializer(stringSerializer);
    template.setHashValueSerializer(stringSerializer);

    template.afterPropertiesSet();
    return template;
}
```

**配置说明**:
- `StringRedisSerializer`: 字符串序列化器，使用UTF-8编码
- `setKeySerializer`: key序列化器
- `setValueSerializer`: value序列化器
- `setHashKeySerializer`: hash key序列化器
- `setHashValueSerializer`: hash value序列化器

### 3. StringRedisTemplate配置

```java
@Bean
public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
    StringRedisTemplate template = new StringRedisTemplate();
    template.setConnectionFactory(factory);

    // 确保使用UTF-8编码
    StringRedisSerializer stringSerializer = 
        new StringRedisSerializer(StandardCharsets.UTF_8);
    
    template.setKeySerializer(stringSerializer);
    template.setValueSerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setHashValueSerializer(stringSerializer);

    template.afterPropertiesSet();
    return template;
}
```

---

## 验证方法

### 1. Redis客户端验证

使用Redis客户端（如redis-cli、RedisInsight）查看数据：

```bash
# 连接Redis
redis-cli -h 192.168.10.109 -p 6379 -a drk@2025

# 查看所有key
KEYS *

# 查看特定key
GET cache:用户:123

# 应该看到正常的中文字符，而不是乱码
```

### 2. 单元测试验证

运行中文支持测试用例：

```bash
./gradlew test --tests ChineseCharacterSupportTest
```

**测试覆盖**:
- ✅ 中文key缓存操作
- ✅ 中文value缓存操作
- ✅ 混合中英文缓存操作
- ✅ 特殊Unicode字符支持（Emoji等）
- ✅ 批量中文缓存操作
- ✅ 中文前缀删除操作
- ✅ 带加载器的中文缓存

---

## 使用示例

### 1. 缓存中文数据

```java
@Autowired
private TwoLevelCacheService cacheService;

// 使用中文key
String chineseKey = "用户:123";
TestUser user = new TestUser("user-123", "张三");

// 设置缓存（key和value都支持中文）
cacheService.set(chineseKey, user, 30, TimeUnit.MINUTES);

// 获取缓存
TestUser cached = cacheService.get(chineseKey, TestUser.class);
// 在Redis客户端中显示为: cache:用户:123 -> {"id":"user-123","name":"张三"}
```

### 2. 混合中英文

```java
// 混合key和value
String key = "cache:用户:test:001";
TestUser user = new TestUser("MIX-001", "测试用户");
user.setAddress("Test Address: 测试地址");

cacheService.set(key, user, 30, TimeUnit.MINUTES);

// Redis客户端显示正常
```

### 3. 特殊Unicode字符

```java
// 支持Emoji等特殊字符
String key = "cache:emoji:😀";
TestUser user = new TestUser("UNI-001", "用户😀");
user.setRemark("特殊字符!@#$%^&*()");

cacheService.set(key, user, 30, TimeUnit.MINUTES);

// Redis客户端显示正常
```

---

## 配置清单

### 必需配置

| 配置项 | 说明 | 状态 |
|--------|------|------|
| Redisson StringCodec | Redisson客户端使用StringCodec | ✅ 已配置 |
| RedisTemplate StringSerializer | RedisTemplate使用StringRedisSerializer | ✅ 已配置 |
| UTF-8字符集 | 确保使用UTF-8编码 | ✅ 已配置 |

### 配置文件

1. `RedissonConfiguration.java` - Redisson客户端配置
2. `ChineseCharacterSupportTest.java` - 中文支持测试用例

---

## 注意事项

### 1. 字符集一致性

确保所有组件使用相同的字符集（UTF-8）：

```java
// ✅ 正确：所有地方使用UTF-8
new StringCodec(StandardCharsets.UTF_8)
new StringRedisSerializer(StandardCharsets.UTF_8)

// ❌ 错误：使用不同字符集
new StringCodec(StandardCharsets.ISO_8859_1)
new StringRedisSerializer(StandardCharsets.US_ASCII)
```

### 2. JSON序列化

对于JSON对象，使用Jackson的UTF-8编码：

```java
ObjectMapper mapper = new ObjectMapper();
// 默认使用UTF-8，支持中文
String json = mapper.writeValueAsString(object);
```

### 3. Redis客户端设置

确保Redis客户端使用UTF-8编码显示：

```bash
# redis-cli
redis-cli --raw  # 使用原始输出，避免转义问题

# 或设置编码
redis-cli --utf8  # 使用UTF-8编码
```

---

## 常见问题

### Q1: 为什么还是看到乱码？

**检查清单**:
1. 确认Redisson配置使用了`setDefaultCodec(new StringCodec(StandardCharsets.UTF_8))`
2. 确认RedisTemplate配置使用了`StringRedisSerializer`
3. 重启应用
4. 清空旧数据（重新写入）

### Q2: JSON中的中文如何处理？

**解决方案**:
使用Jackson的默认UTF-8编码：

```java
ObjectMapper mapper = new ObjectMapper();
// Jackson默认使用UTF-8，直接支持中文
```

### Q3: 如何支持更多Unicode字符（如Emoji）？

**解决方案**:
StringCodec使用UTF-8编码，天然支持所有Unicode字符，包括Emoji、特殊符号等。

---

## 测试验证

### 运行测试

```bash
# 运行中文支持测试
cd examples
./gradlew test --tests ChineseCharacterSupportTest

# 查看测试报告
open build/reports/tests/test/index.html
```

### 预期结果

所有测试用例应该通过：
- ✅ ZH-CN-001: 中文key缓存操作
- ✅ ZH-CN-002: 中文value缓存操作
- ✅ ZH-CN-003: 混合中英文缓存操作
- ✅ ZH-CN-004: 特殊Unicode字符支持
- ✅ ZH-CN-005: 批量中文缓存操作
- ✅ ZH-CN-006: 中文前缀删除操作
- ✅ ZH-CN-007: 带加载器的中文缓存

---

## 配置效果

### 配置前（乱码）

```bash
redis> KEYS *
1) "\xAC\xED\x00\x05t\x00\x05cache:user:123"
2) "\xAC\xED\x00\x05t\x00\x07user:info"

redis> GET "\xAC\xED\x00\x05t\x00\x05cache:user:123"
"\xAC\xED\x00\x05t\x00\x0Bserialized_data"
```

### 配置后（正常显示）

```bash
redis> KEYS *
1) "cache:用户:123"
2) "cache:user:info"

redis> GET cache:用户:123
"{\"id\":\"123\",\"name\":\"张三\",\"address\":\"北京市\"}"
```

---

**维护**: Redis ToolKit Team  
**更新**: 2026-06-06  
**版本**: v1.0.0
