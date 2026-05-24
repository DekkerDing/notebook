# Redis工具集安全规范

---

## 1. Redis连接安全

### 1.1 密码管理规范

#### 密码存储【强制】

- 【禁止】在代码中硬编码Redis密码
- 【禁止】将密码提交到版本控制系统
- 【强制】密码必须通过环境变量或配置中心获取

```yaml
# ❌ 禁止：硬编码密码
spring:
  redis:
    password: "drk@2025"  # 永远不要这样写！

# ✅ 正确：使用环境变量
spring:
  redis:
    password: ${REDIS_PASSWORD}
```

#### 密码复杂度要求

| 要求 | 规则 | 示例 |
|------|------|------|
| 长度 | 最少16位 | `MyStr0ng_P@ssw0rd!` |
| 复杂度 | 包含大小写字母、数字、特殊字符 | `aA1@#bb` |
| 禁止 | 使用常见密码、字典单词 | ❌ `password123` |

### 1.2 TLS/SSL连接【生产环境强制】

```yaml
spring:
  redis:
    ssl: true
    # Lettuce SSL配置
    lettuce:
      pool:
        ssl: true
```

#### SSL配置规范

```java
/**
 * Redis SSL配置
 */
@Configuration
public class RedisSslConfig {

    @Bean
    public LettuceClientConfigurationBuilder redisClientConfiguration() {
        return LettuceClientConfiguration.builder()
            .useSsl()
            .and()
            .commandTimeout(Duration.ofSeconds(5))
            .shutdownTimeout(Duration.ofMillis(100))
            .build();
    }
}
```

### 1.3 连接池安全限制

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 50          # 防止单个应用耗尽连接
        max-idle: 20
        min-idle: 5
        max-wait: 1000         # 防止连接请求堆积
```

---

## 2. Redis Key访问控制

### 2.1 Key命名规范

**前缀隔离【强制】**:

```java
// ✅ 正确：使用应用前缀隔离
redis-toolkit:
  stream:
    key-prefix: "myapp:stream:"  # 命名空间隔离
  lock:
    key-prefix: "myapp:lock:"    # 命名空间隔离
```

**Key命名格式**:
```
{应用前缀}:{功能域}:{业务标识}:{可选后缀}
```

| 示例 | 说明 | 长度控制 |
|------|------|---------|
| `myapp:lock:order:123` | 订单锁 | < 250字符 |
| `myapp:cache:user:456` | 用户缓存 | < 250字符 |

### 2.2 Key权限控制

#### 防止Key冲突【强制】

不同功能模块使用不同的Key前缀：

```java
// ✅ 正确：不同模块使用不同前缀
String lockKey = "myapp:lock:order:" + orderId;
String cacheKey = "myapp:cache:user:" + userId;
String streamKey = "myapp:stream:order:";

// ❌ 错误：可能导致冲突
String key = "resource:" + id;  // 模糊的前缀
```

#### 敏感Key命名

```java
/**
 * 敏感数据Key命名规范
 */
public class SecureKeyNaming {

    // ✅ 正确：使用hash存储敏感数据
    public String getUserKey(String userId) {
        return "myapp:user:" + userId + ":profile";
    }

    // ✅ 正确：敏感字段使用单独hash
    public String getPasswordField() {
        return "pwd";  // 不直接在key中暴露
    }
}
```

---

## 3. 数据安全

### 3.1 敏感数据加密

#### 加密规范【强制】

**必须加密的敏感数据**:
- 密码
- 身份证Token
- 个人信息（PII）
- 业务敏感数据

```java
/**
 * 敏感数据加密工具
 */
public class SensitiveDataEncryption {

    private final AESUtil aesUtil;

    /**
     * 加密敏感数据后存储
     */
    public void storeSensitive(String key, String plainText) {
        String encrypted = aesUtil.encrypt(plainText);
        redisTemplate.opsForValue().set(key, encrypted);
    }

    /**
     * 获取并解密敏感数据
     */
    public String getSensitive(String key) {
        String encrypted = redisTemplate.opsForValue().get(key);
        return aesUtil.decrypt(encrypted);
    }
}
```

#### 加密算法选择

| 数据类型 | 加密算法 | 密钥长度 |
|---------|---------|---------|
| 一般敏感数据 | AES-128-GCM | 128位 |
| 高敏感数据 | AES-256-GCM | 256位 |

```java
/**
 * AES加密配置
 */
@Configuration
public class AesEncryptionConfig {

    @Bean
    public AESUtil aesUtil() {
        return new AESUtil(
            env.getProperty("aes.secret-key"),  // 从环境变量读取
            "AES/GCM/NoPadding"
        );
    }
}
```

### 3.2 数据脱敏

#### 日志脱敏【强制】

```java
/**
 * 数据脱敏工具
 */
public class DataMaskingUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(.{2})[^@]+(@.{2})");

    /**
     * 手机号脱敏
     */
    public static String maskPhone(String phone) {
        if (StringUtils.isBlank(phone)) return phone;
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    /**
     * 身份证脱敏
     */
    public static String maskIdCard(String idCard) {
        if (StringUtils.isBlank(idCard)) return idCard;
        return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1********$2");
    }

    /**
     * 邮箱脱敏
     */
    public static String maskEmail(String email) {
        if (StringUtils.isBlank(email)) return email;
        return EMAIL_PATTERN.matcher(email).replaceAll("$1***$2");
    }
}
```

#### 日志输出规范

```java
// ✅ 正确：日志输出前脱敏
log.info("User login: userId={}, phone={}, email={}",
    userId,
    DataMaskingUtil.maskPhone(user.getPhone()),
    DataMaskingUtil.maskEmail(user.getEmail()));

// ❌ 错误：日志输出敏感信息
log.info("User login: user={}", user);  // 可能包含敏感信息
```

---

## 4. 命令执行安全

### 4.1 防止命令注入

#### 危险命令黑名单【强制】

```java
/**
 * 危险命令黑名单
 */
public class DangerousCommandBlacklist {

    private static final Set<String> BLACKLIST = new HashSet<>(Arrays.asList(
        "FLUSHDB",      // 清空数据库
        "FLUSHALL",     // 清空所有数据库
        "KEYS *",      // 列出所有Key
        "CONFIG SET",  // 修改配置
        "SHUTDOWN",     // 关闭Redis
        "EVAL"         // 执行任意代码
    ));

    /**
     * 检查命令是否危险
     */
    public static boolean isDangerous(String command) {
        String upperCmd = command.toUpperCase().trim();
        return BLACKLIST.stream().anyMatch(dangerous -> upperCmd.contains(dangerous));
    }

    /**
     * 校验Redis命令
     */
    public static void validateCommand(String command) {
        if (isDangerous(command)) {
            throw new SecurityException("DANGEROUS_COMMAND", 
                "禁止执行危险命令: " + command);
        }
    }
}
```

### 4.2 Lua脚本安全

#### Lua脚本规范【强制】

**禁止事项**:
- ❌ Lua脚本中使用全局变量
- ❌ Lua脚本中执行无限循环
- ❌ Lua脚本中调用危险的Redis命令

```java
/**
 * Lua脚本安全管理器
 */
public class LuaScriptSecurityManager {

    /**
     * 安全的Lua脚本加载
     */
    public void loadScript(String scriptName, String scriptContent) {
        // 1. 检查脚本长度
        if (scriptContent.length() > 10000) {
            throw new SecurityException("SCRIPT_TOO_LARGE", 
                "Lua脚本过长，可能包含恶意逻辑");
        }

        // 2. 检查危险关键字
        String[] dangerousKeywords = {
            "redis.call('FLUSH",
            "redis.call('KEYS",
            "while true do",  // 无限循环
            "os.execute"      // 执行系统命令
        };

        for (String keyword : dangerousKeywords) {
            if (scriptContent.toLowerCase().contains(keyword)) {
                throw new SecurityException("DANGEROUS_SCRIPT", 
                    "Lua脚本包含危险操作: " + keyword);
            }
        }

        // 3. 加载脚本
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.scriptingCommands().scriptLoad(
                scriptName.getBytes(),
                scriptContent.getBytes()
            );
            return null;
        });
    }
}
```

#### Lua脚本白名单

```java
/**
 * Lua脚本白名单管理
 */
@Component
public class LuaScriptWhitelist {

    private final Set<String> allowedScripts = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 预加载允许的脚本
        loadAllowedScript("unlock.lua", loadResource("scripts/unlock.lua"));
        loadAllowedScript("renew.lua", loadResource("scripts/renew.lua"));
    }

    /**
     * 检查脚本是否在白名单中
     */
    public boolean isAllowed(String scriptName) {
        return allowedScripts.contains(scriptName);
    }

    /**
     * 添加到白名单
     */
    public void addToWhitelist(String scriptName, String scriptDigest) {
        allowedScripts.add(scriptName);
        log.info("Lua script added to whitelist: name={}, digest={}", 
            scriptName, scriptDigest);
    }
}
```

---

## 5. 资源限制保护

### 5.1 资源消耗限制

#### 内存使用限制

```yaml
redis-toolkit:
  resource-limits:
    # 最大内存使用量
    max-memory-percent: 80
    # 单个Value最大大小
    max-value-size: 10MB
    # 批量操作最大数量
    max-batch-size: 1000
```

```java
/**
 * 资源限制检查器
 */
@Component
public class ResourceLimitChecker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ResourceLimitProperties properties;

    /**
     * 检查Value大小
     */
    public void checkValueSize(byte[] value) {
        long maxSize = properties.getMaxValueSize() * 1024 * 1024;  // 转换为字节
        if (value.length > maxSize) {
            throw new ResourceExceededException("VALUE_TOO_LARGE",
                "Value size " + value.length + " exceeds limit " + maxSize);
        }
    }

    /**
     * 检查批量大小
     */
    public void checkBatchSize(int batchSize) {
        int maxBatchSize = properties.getMaxBatchSize();
        if (batchSize > maxBatchSize) {
            throw new ResourceExceededException("BATCH_TOO_LARGE",
                "Batch size " + batchSize + " exceeds limit " + maxBatchSize);
        }
    }
}
```

### 5.2 操作频率限制

#### 操作频率控制

```java
/**
 * 操作频率限制器
 */
@Component
public class OperationRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitProperties properties;

    /**
     * 检查操作频率
     */
    public boolean checkRateLimit(String operation, String key) {
        String limitKey = "ratelimit:" + operation + ":" + key;

        Long current = redisTemplate.opsForValue().increment(limitKey);
        if (current == null) {
            redisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
            return true;
        }

        long maxOperations = properties.getMaxOperationsPerMinute();
        if (current > maxOperations) {
            log.warn("Operation rate limit exceeded: operation={}, key={}, current={}", 
                operation, key, current);
            return false;
        }

        return true;
    }
}
```

---

## 6. 网络安全

### 6.1 访问控制

#### IP白名单（可选）

```yaml
redis-toolkit:
  security:
    access-control:
      enabled: true
      allowed-ips:
        - "10.0.0.0/8"
        - "192.168.0.0/16"
```

```java
/**
 * IP访问控制
 */
@Component
@ConditionalOnProperty(prefix = "redis-toolkit.security.access-control", name = "enabled", havingValue = "true")
public class IpAccessControlInterceptor {

    private final List<String> allowedIps;

    /**
     * 检查IP是否在白名单中
     */
    public boolean isAllowed(String clientIp) {
        return allowedIps.stream().anyMatch(allowed -> {
            if (allowed.contains("/")) {
                String[] parts = allowed.split("/");
                String subnet = parts[0];
                int prefix = Integer.parseInt(parts[1]);
                return isIpInSubnet(clientIp, subnet, prefix);
            }
            return clientIp.equals(allowed);
        });
    }
}
```

### 6.2 防火墙规则

```yaml
# Redis防火墙规则示例
redis-toolkit:
  security:
    firewall:
      # 禁止的危险命令
      blocked-commands:
        - FLUSHDB
        - FLUSHALL
        - CONFIG SET
      # 允许的命令前缀
      allowed-command-prefixes:
        - GET
        - SET
        - HSET
        - XADD
        - XREADGROUP
```

---

## 7. 安全审计

### 7.1 操作日志

#### 必须记录的安全事件

| 事件类型 | 记录内容 | 日志级别 |
|---------|---------|---------|
| 认证失败 | 用户、时间、IP、失败原因 | WARN |
| 授权失败 | 操作、资源、用户、时间 | WARN |
| 危险操作 | FLUSH、CONFIG、KEYS | ERROR |
| 异常访问 | 操作、Key、用户、时间 | WARN |
| 资源超限 | 操作类型、限制、时间 | WARN |

```java
/**
 * 安全审计日志
 */
@Slf4j
@Component
public class SecurityAuditLogger {

    /**
     * 记录安全事件
     */
    public void logSecurityEvent(SecurityEvent event) {
        log.warn("SECURITY_EVENT: type={}, operation={}, resource={}, user={}, ip={}, result={}",
            event.getType(),
            event.getOperation(),
            event.getResource(),
            event.getUser(),
            event.getIp(),
            event.getResult()
        );

        // 持久化到专门的审计日志
        persistAuditLog(event);
    }

    /**
     * 持久化审计日志
     */
    private void persistAuditLog(SecurityEvent event) {
        String auditKey = "audit:security:" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(auditKey, event.toJson(), 7, TimeUnit.DAYS);
    }
}
```

### 7.2 异常行为检测

```java
/**
 * 异常行为检测器
 */
@Component
public class AnomalyDetection {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 检测异常高频访问
     */
    public boolean detectHighFrequencyAccess(String key, String operation, int threshold) {
        String counterKey = "anomaly:frequency:" + operation + ":" + key;
        Long count = redisTemplate.opsForValue().increment(counterKey);
        redisTemplate.expire(counterKey, 1, TimeUnit.MINUTES);

        if (count > threshold) {
            log.warn("高频访问检测: key={}, operation={}, count={}", 
                key, operation, count);
            return true;
        }
        return false;
    }

    /**
     * 检测异常数据模式
     */
    public boolean detectAnomalousPattern(String key, String pattern) {
        // 实现异常模式检测逻辑
        // 例如：短时间内访问大量不同的Key
        return false;
    }
}
```

---

## 8. 密钥管理

### 8.1 密钥轮换

#### 定期轮换策略

```yaml
redis-toolkit:
  security:
    key-rotation:
      enabled: true
      rotation-interval: 90  # 天
      notify-before: 7          # 天
```

### 8.2 密钥托管

```java
/**
 * 密钥托管服务
 */
@Service
@ConditionalOnProperty(prefix = "redis-toolkit.security.key-rotation", name = "enabled", havingValue = "true")
public class KeyManagementService {

    private final VaultClient vaultClient;

    /**
     * 从密钥托管服务获取密码
     */
    public String getPassword() {
        try {
            return vaultClient.read("secret/redis/password");
        } catch (Exception e) {
            log.error("从密钥托管获取密码失败", e);
            throw new SecurityException("KEY_FETCH_FAILED", 
                "无法从密钥托管服务获取密码");
        }
    }

    /**
     * 更新Redis密码
     */
    public void rotatePassword(String newPassword) {
        vaultClient.write("secret/redis/password", newPassword);
        log.info("Redis密码已更新");
    }
}
```

---

## 9. 合规性要求

### 9.1 数据保护合规

| 法规 | 要求 | 实现方式 |
|------|------|---------|
| **GDPR** | 数据最小化、用户权利 | 敏感数据加密、数据删除 |
| **等保2.0** | 数据分级、访问控制 | 数据分类、访问审计 |
| **网络安全法** | 数据加密、日志留存 | TLS传输、操作日志 |

### 9.2 数据最小化

```java
/**
 * 数据最小化工具
 */
public class DataMinimization {

    /**
     * 只返回必要的字段
     */
    public <T> T minimizeData(T fullData, String... requiredFields) {
        // 实现数据最小化逻辑
        // 只包含必需的字段，敏感字段脱敏
        return fullData; // TODO: 实现
    }
}
```

---

## 10. 安全检查清单

### 开发阶段

- [ ] 密码不硬编码
- [ ] 敏感数据加密存储
- [ ] 日志输出脱敏
- [ ] 危险命令检查
- [ ] Lua脚本安全检查
- [ ] 资源限制配置

### 部署阶段

- [ ] 使用TLS/SSL连接
- [ ] 配置密码复杂度要求
- [ ] 配置网络隔离
- [ ] 配置防火墙规则
- [ ] 启用安全审计日志
- [ ] 配置异常检测

### 运维阶段

- [ ] 定期轮换密码
- [ ] 监控异常访问
- [ ] 审计日志分析
- [ ] 漏洞扫描
- [ ] 安全渗透测试

---

## 11. 安全配置示例

### 生产环境配置

```yaml
# 生产环境安全配置
redis-toolkit:
  enabled: true
  
  # 连接安全
  connection:
    ssl: true
    timeout: 5000
    password: ${REDIS_PASSWORD}
  
  # 资源限制
  resource-limits:
    max-memory-percent: 80
    max-value-size: 10MB
    max-batch-size: 1000
  
  # 安全审计
  security:
    audit:
      enabled: true
      retention-days: 90
    
    # 密钥轮换
    key-rotation:
      enabled: true
      rotation-interval: 90
      notify-before: 7
    
    # 异常检测
    anomaly-detection:
      enabled: true
      high-frequency-threshold: 1000
```

---

**文档维护**: Redis工具集团队  
**更新频率**: 每次安全规范变更后  
**下次审查**: 季度安全审查
