# 配置文件与环境管理规范

---

## 1. 配置文件结构

### 1.1 工具库配置目录

```
redis-toolkit-api/src/main/resources/
├── META-INF/
│   └── spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports  # 自动配置注册
└── redis-toolkit-defaults.yml                                               # 默认配置值
```

### 1.2 应用使用示例

```yaml
# 应用项目的 application.yml
redis-toolkit:
  enabled: true
  stream:
    event:
      enabled: true
      key-prefix: "myapp:stream:"
      partition:
        enabled: true
        count: 3
        strategy: hash
  lock:
    key-prefix: "myapp:lock:"
    default-lease-time: 30000
```

---

## 2. 配置项命名规范

- 【强制】使用kebab-case（短横线命名），按模块分组
- 【强制】配置前缀统一为 `redis-toolkit`

```yaml
redis-toolkit:
  # Stream事件配置
  stream:
    event:
      enabled: true
      key-prefix: "stream:"
      poll-timeout: 2000
      batch-size: 10

  # 分布式锁配置
  lock:
    key-prefix: "lock:"
    default-lease-time: 30000
    default-wait-time: 10000

  # 缓存配置
  cache:
    key-prefix: "cache:"
    default-ttl: 600

  # 限流配置
  rate-limit:
    key-prefix: "ratelimit:"
```

---

## 3. 配置属性类规范

### 3.1 根配置类

```java
/**
 * Redis工具包配置属性
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "redis-toolkit")
@Validated
public class RedisToolkitProperties {

    /**
     * 是否启用Redis工具包
     */
    private boolean enabled = true;

    /**
     * Stream事件配置
     */
    @Valid
    private Stream stream = new Stream();

    /**
     * 分布式锁配置
     */
    @Valid
    private Lock lock = new Lock();

    /**
     * 缓存配置
     */
    @Valid
    private Cache cache = new Cache();
}
```

### 3.2 子配置类模板

```java
/**
 * Stream配置属性
 */
@Data
public static class Stream {

    /**
     * 是否启用Stream功能
     */
    private boolean enabled = true;

    /**
     * Stream Key前缀
     */
    private String keyPrefix = "stream:";

    /**
     * 默认消费者组名
     */
    private String defaultGroup = "default-group";

    /**
     * 轮询超时时间（毫秒）
     */
    @Min(100)
    @Max(60000)
    private long pollTimeout = 2000;

    /**
     * 批量消费大小
     */
    @Min(1)
    @Max(1000)
    private int batchSize = 10;

    /**
     * 分区配置
     */
    @Valid
    private Partition partition = new Partition();

    @Data
    public static class Partition {
        private boolean enabled = false;
        @Min(1)
        @Max(100)
        private int count = 3;
        private String strategy = "hash";
    }
}
```

### 3.3 配置属性规则

| 规则 | 说明 | 示例 |
|------|------|------|
| prefix使用kebab-case | 与yaml中key完全对应 | `redis-toolkit.stream.event` |
| 提供默认值 | 防止配置缺失导致启动失败 | `private int batchSize = 10` |
| 添加@Min/@Max校验 | 限制数值范围 | `@Min(1) @Max(1000)` |
| 使用@Valid | 嵌套对象校验 | `@Valid private Partition partition` |
| 禁止注入Bean | Properties类只存配置 | 禁止@Autowired |

---

## 4. 自动配置规范

### 4.1 自动配置类模板

```java
/**
 * Redis工具包自动配置
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@ConditionalOnProperty(prefix = "redis-toolkit", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(RedisToolkitProperties.class)
public class RedisStreamEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EventSerializer eventSerializer() {
        return new JacksonEventSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisTemplate.class)
    public EventPublishingService eventPublishingService(
            RedisTemplate<String, String> redisTemplate,
            EventSerializer serializer,
            RedisToolkitProperties properties) {
        return new OptimizedRedisStreamPublisher(redisTemplate, serializer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisTemplate.class)
    public StreamConsumerRegistry streamConsumerRegistry(
            RedisTemplate<String, String> redisTemplate,
            EventSerializer serializer,
            RedisToolkitProperties properties) {
        return new StreamConsumerRegistry(redisTemplate, serializer, properties);
    }
}
```

### 4.2 条件注解使用

| 注解 | 使用场景 | 示例 |
|------|---------|------|
| `@ConditionalOnProperty` | 配置开关 | `enabled=true`时生效 |
| `@ConditionalOnMissingBean` | 用户自定义Bean优先 | 允许用户覆盖默认实现 |
| `@ConditionalOnBean` | 依赖存在时生效 | RedisTemplate存在时 |
| `@ConditionalOnClass` | 类路径存在时生效 | Redisson存在时 |

### 4.3 自动配置注册

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.github.dekkerding.redis.config.RedisStreamEventAutoConfiguration
io.github.dekkerding.redis.config.RedisLockAutoConfiguration
io.github.dekkerding.redis.config.RedisCacheAutoConfiguration
```

---

## 5. 环境管理规范

### 5.1 环境标识

| 环境 | Profile | 说明 |
|------|---------|------|
| 本地开发 | `local` | 本地Redis，无密码 |
| 开发环境 | `dev` | 开发服务器Redis |
| 测试环境 | `test` | 测试服务器Redis |
| 生产环境 | `prod` | 生产Redis集群 |

### 5.2 各环境配置示例

**application-local.yml**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:

redis-toolkit:
  stream:
    event:
      poll-timeout: 1000
```

**application-prod.yml**
```yaml
spring:
  redis:
    cluster:
      nodes:
        - 192.168.10.109:6379
        - 192.168.10.107:6379
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 50
        max-idle: 20

redis-toolkit:
  stream:
    event:
      poll-timeout: 2000
      batch-size: 20
      partition:
        enabled: true
        count: 5
```

---

## 6. Redis连接配置规范

### 6.1 Lettuce客户端配置

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: ${REDIS_DATABASE:0}
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
        max-wait: 1000ms
      shutdown-timeout: 100ms
```

### 6.2 Redisson客户端配置

```yaml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://${REDIS_HOST:localhost}:${REDIS_PORT:6379}"
          password: ${REDIS_PASSWORD:}
          database: ${REDIS_DATABASE:0}
          connectionMinimumIdleSize: 5
          connectionPoolSize: 20
          connectTimeout: 5000
          timeout: 10000
          retryAttempts: 3
          retryInterval: 1500
```

### 6.3 集群配置

```yaml
spring:
  redis:
    cluster:
      nodes:
        - 192.168.10.109:6379
        - 192.168.10.107:6379
      max-redirects: 3
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 100
        max-idle: 50
```

---

## 7. 配置元数据

### 7.1 spring-configuration-metadata.json

工具库应提供配置元数据，支持IDE自动提示：

```json
{
  "groups": [
    {
      "name": "redis-toolkit.stream",
      "type": "io.github.dekkerding.redis.config.RedisToolkitProperties$Stream",
      "sourceType": "io.github.dekkerding.redis.config.RedisToolkitProperties",
      "description": "Redis Stream事件配置。"
    }
  ],
  "properties": [
    {
      "name": "redis-toolkit.stream.event.enabled",
      "type": "java.lang.Boolean",
      "defaultValue": true,
      "description": "是否启用Stream事件功能。"
    },
    {
      "name": "redis-toolkit.stream.event.poll-timeout",
      "type": "java.lang.Long",
      "defaultValue": 2000,
      "description": "XREADGROUP轮询超时时间（毫秒）。"
    }
  ],
  "hints": []
}
```

---

## 8. 配置校验规范

### 8.1 启动时校验

```java
@Component
@Data
@ConfigurationProperties(prefix = "redis-toolkit.stream.event")
@Validated
public class StreamEventProperties {

    /**
     * 轮询超时时间
     */
    @Min(100)
    @Max(60000)
    private Long pollTimeout = 2000L;

    /**
     * 批量大小
     */
    @Min(1)
    @Max(1000)
    private Integer batchSize = 10;

    /**
     * 分区数量
     */
    @Min(1)
    @Max(100)
    private Integer partitionCount = 3;
}
```

### 8.2 运行时校验

```java
public class PartitionManager {

    public void validatePartitionCount(int count) {
        if (count < 1 || count > 100) {
            throw new RedisException("INVALID_CONFIG",
                "分区数量必须在1-100之间: " + count);
        }
    }
}
```

---

## 9. 敏感信息处理

### 9.1 密码管理规范

- 【强制】Redis密码必须通过环境变量或配置中心获取
- 【禁止】密码硬编码在代码中
- 【禁止】密码明文提交到版本控制

```yaml
# ✅ 正确：使用环境变量
spring:
  redis:
    password: ${REDIS_PASSWORD}

# ❌ 错误：硬编码密码
spring:
  redis:
    password: "drk@2025"
```

### 9.2 日志脱敏

```java
@Slf4j
public class RedisConfigLogger {

    public void logConnectionConfig(String host, int port, String password) {
        log.info("Redis连接配置: host={}, port={}, password={}",
            host, port, maskPassword(password));
    }

    private String maskPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return "";
        }
        return password.substring(0, 1) + "****" +
               password.substring(password.length() - 1);
    }
}
```

---

## 10. 配置迁移指南

### 10.1 版本升级

从0.x升级到1.0.0时的配置变更：

```yaml
# 0.x 配置
spring:
  redis:
    stream:
      event:
        enabled: true

# 1.0.0 配置
redis-toolkit:
  stream:
    event:
      enabled: true
```

### 10.2 废弃配置处理

```java
/**
 * 配置兼容处理器
 */
@Component
public class ConfigCompatibilityHandler {

    @EventListener(ApplicationReadyEvent.class)
    public void checkDeprecatedConfig() {
        // 检测旧配置并提示迁移
        if (StringUtils.hasText(environment.getProperty("spring.redis.stream.event.enabled"))) {
            log.warn("检测到旧版配置，请迁移到 redis-toolkit.stream.event.enabled");
        }
    }
}
```

---

**文档维护**: Redis工具集团队
**更新频率**: 每次配置变更后
**下次审查**: 版本发布前