# 单元测试与集成测试规范

---

## 1. 测试原则

### 1.1 核心原则

| 原则 | 说明 |
|------|------|
| **F**ast | 测试执行要快，单个测试 < 100ms |
| **I**solated | 测试间相互独立，无共享状态 |
| **R**epeatable | 任意环境、任意时间执行结果一致 |
| **S**elf-validating | 测试结果非 Pass 即 Fail，无需人工判断 |
| **T**imely | 与业务代码同步编写，不事后补测 |

### 1.2 测试分层策略

```
┌─────────────────────────────────────┐
│   E2E Tests (端到端测试)            │  ← 少量，关键路径
├─────────────────────────────────────┤
│   Integration Tests (集成测试)       │  ← 中等，验证组件协作
├─────────────────────────────────────┤
│   Unit Tests (单元测试)              │  ← 大量，验证业务逻辑
└─────────────────────────────────────┘
```

---

## 2. 单元测试规范

### 2.1 命名规范

**测试类命名**
```
{被测类名}Test.java
例：RedisStreamPublisherTest.java
```

**测试方法命名** — 采用 `should_预期结果_when_测试条件` 格式：

```java
// 推荐
@Test
void should_return_messageId_when_publish_successfully()

@Test
void should_throw_exception_when_redis_connection_failed()

@Test
void should_return_empty_list_when_no_messages_exist()
```

### 2.2 AAA测试结构

每个测试方法严格遵循三段式结构：

```java
@Test
void should_publish_event_when_valid_input() {
    // Arrange — 准备测试数据和依赖
    DomainEvent event = OrderCreatedEvent.builder()
        .orderId("order-123")
        .amount(BigDecimal.valueOf(100))
        .build();

    when(redisTemplate.opsForStream()).thenReturn(streamOperations);
    when(streamOperations.add(any(), anyMap())).thenReturn(RecordId.of("1-0"));

    // Act — 执行被测方法
    MessageId messageId = publisher.publish(event);

    // Assert — 验证结果
    assertThat(messageId).isNotNull();
    assertThat(messageId.getValue()).isEqualTo("1-0");
}
```

### 2.3 Mock使用规范

```java
// 推荐：只 Mock 外部依赖（Redis、序列化器）
@ExtendWith(MockitoExtension.class)
class RedisStreamPublisherTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;   // Mock 外部依赖 ✓

    @Mock
    private EventSerializer eventSerializer;               // Mock 外部依赖 ✓

    @InjectMocks
    private OptimizedRedisStreamPublisher publisher;       // 真实被测对象

    // 禁止 Mock 被测类自身的方法 ✗
    // 禁止 Mock 简单的值对象、DTO ✗
}
```

### 2.4 覆盖率要求

| 层级 | 行覆盖率 | 分支覆盖率 | 说明 |
|------|---------|-----------|------|
| 核心功能（Stream、锁、缓存）| ≥ 80% | ≥ 75% | 核心逻辑必须充分覆盖 |
| 工具类（序列化、分区）| ≥ 90% | ≥ 85% | 工具类必须充分覆盖 |
| 配置类 | ≥ 60% | — | 重点测试条件装配 |

### 2.5 边界与异常测试

每个业务方法必须覆盖以下场景：

```java
// 正常路径
@Test void should_publish_successfully_with_valid_event()

// 边界值
@Test void should_accept_large_event_content()
@Test void should_handle_empty_event_id()

// 异常路径
@Test void should_throw_exception_when_serialization_failed()
@Test void should_throw_exception_when_redis_unavailable()

// 空值处理
@Test void should_return_null_when_event_not_found()
```

### 2.6 测试数据管理

```java
/**
 * 测试数据构建器
 */
public class TestDataBuilder {

    public static OrderCreatedEvent.OrderCreatedEventBuilder orderCreatedEvent() {
        return OrderCreatedEvent.builder()
            .eventId("event-" + UUID.randomUUID())
            .orderId("order-" + RandomStringUtils.randomNumeric(6))
            .amount(BigDecimal.valueOf(RandomUtils.nextLong(10, 10000)))
            .createdAt(LocalDateTime.now());
    }

    public static EventEnvelope.EventEnvelopeBuilder eventEnvelope() {
        return EventEnvelope.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("OrderCreatedEvent")
            .source("test-service")
            .timestamp(Instant.now());
    }
}
```

### 2.7 禁止事项

```
✗ 测试方法内有 System.out.println
✗ 测试方法包含 Thread.sleep（用 Awaitility 替代）
✗ 空的 catch 块或 catch 后直接 return
✗ 一个测试方法验证多个不相关功能
✗ 测试依赖执行顺序
✗ 注释掉失败的测试而不修复
```

---

## 3. 集成测试规范

### 3.1 Testcontainers配置

引入依赖：

```xml
<!-- Testcontainers - 提供真实Redis环境 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### 3.2 Redis集成测试模板

```java
/**
 * Redis Stream 集成测试
 * 使用 Testcontainers 启动真实 Redis，验证 Redis 操作逻辑
 *
 * @author Redis Toolkit Team
 * @since 1.0.0
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RedisStreamIntegrationTest {

    // 所有集成测试共用同一个容器（static），避免每次测试重新启动
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withReuse(true);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private EventPublishingService publishingService;

    @Autowired
    private StreamConsumerRegistry consumerRegistry;

    @Test
    void should_publish_and_consume_event() {
        // Arrange
        OrderCreatedEvent event = TestDataBuilder.orderCreatedEvent().build();

        // Act
        MessageId messageId = publishingService.publish(event);

        // Assert
        assertThat(messageId).isNotNull();
        // 验证消息在Redis中
        // ...
    }

    @Test
    @Transactional
    void should_handle_consumer_group_creation() {
        // 测试消费者组创建逻辑
    }
}
```

### 3.3 集成测试目录约定

```
src/test/java/
└── io/github/dekkerding/redis/
    ├── core/
    │   ├── stream/
    │   │   ├── RedisStreamPublisherTest.java        ← 单元测试
    │   │   └── RedisStreamIntegrationTest.java       ← 集成测试
    │   └── lock/
    │       ├── RedisLockTest.java                    ← 单元测试
    │       └── RedisLockIntegrationTest.java         ← 集成测试
    └── util/
        └── JsonUtilTest.java                         ← 工具类测试
```

### 3.4 集成测试命名约定

- 单元测试：`XxxTest.java`
- 集成测试：`XxxIT.java` 或 `XxxIntegrationTest.java`

### 3.5 集成测试禁止行为

```
✗ 禁止集成测试依赖真实外网服务（使用 Testcontainers 替代）
✗ 禁止集成测试写死 IP/端口（使用动态端口）
✗ 禁止集成测试污染其他测试数据（使用 @AfterEach 清理）
✗ 禁止在 CI/CD 中跳过集成测试
```

---

## 4. 测试工具类

### 4.1 Redis测试工具

```java
/**
 * Redis测试工具类
 */
@Component
public class RedisTestUtils {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 清空所有测试数据
     */
    public void flushAll() {
        redisTemplate.getConnectionFactory()
            .getConnection()
            .flushDb();
    }

    /**
     * 获取Stream中的消息数量
     */
    public long getStreamLength(String streamKey) {
        return redisTemplate.opsForStream().size(streamKey);
    }

    /**
     * 读取Stream中的所有消息
     */
    public List<MapRecord<String, Object, Object>> readAllMessages(String streamKey) {
        return redisTemplate.opsForStream()
            .range(streamKey, Range.unbounded());
    }

    /**
     * 删除指定Stream
     */
    public Boolean deleteStream(String streamKey) {
        return redisTemplate.delete(streamKey);
    }
}
```

### 4.2 等待工具（Awaitility）

```java
@Test
void should_consume_message_within_timeout() {
    // 发布消息
    publisher.publish(event);

    // 等待消息被消费
    await().atMost(5, TimeUnit.SECONDS)
        .until(() -> consumer.getProcessedCount() > 0);

    assertThat(consumer.getProcessedCount()).isEqualTo(1);
}
```

---

## 5. 性能测试规范

### 5.1 JMH性能测试

```java
/**
 * Stream发布性能测试（JMH）
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class StreamPublishBenchmark {

    private EventPublishingService publisher;
    private DomainEvent event;

    @Setup
    public void setup() {
        // 初始化测试环境
    }

    @Benchmark
    public MessageId benchmarkPublish() {
        return publisher.publish(event);
    }
}
```

### 5.2 简单性能测试

```java
@Test
void should_handle_high_throughput_publish() {
    int count = 1000;
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < count; i++) {
        publisher.publish(TestDataBuilder.orderCreatedEvent().build());
    }

    long duration = System.currentTimeMillis() - startTime;
    double tps = (count * 1000.0) / duration;

    log.info("Publish {} messages in {}ms, TPS: {}", count, duration, tps);
    assertThat(tps).isGreaterThan(1000); // 目标 TPS > 1000
}
```

---

## 6. 测试配置

### 6.1 测试配置文件

```yaml
# application-test.yml
spring:
  redis:
    host: localhost
    port: 6379
    database: 15  # 使用独立的测试数据库

redis-toolkit:
  enabled: true
  stream:
    event:
      enabled: true
      poll-timeout: 1000

logging:
  level:
    io.github.dekkerding.redis: DEBUG
```

### 6.2 测试配置类

```java
/**
 * 测试配置
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public EventSerializer testEventSerializer() {
        return new JacksonEventSerializer();
    }
}
```

---

## 7. 测试最佳实践

### 7.1 测试隔离

```java
// 每个测试方法前清理数据
@BeforeEach
void setUp() {
    redisTestUtils.flushAll();
}

// 每个测试方法后清理数据
@AfterEach
void tearDown() {
    redisTestUtils.flushAll();
}
```

### 7.2 测试数据独立性

```java
// 每个测试使用独立的Stream Key
@Test
void test_case_1() {
    String streamKey = "test:stream:" + UUID.randomUUID();
    // ...
}

@Test
void test_case_2() {
    String streamKey = "test:stream:" + UUID.randomUUID();
    // ...
}
```

### 7.3 异常断言

```java
// 断言抛出特定异常
assertThatThrownBy(() -> publisher.publish(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("event must not be null");

// 断言异常消息
assertThatExceptionOfType(EventPublishException.class)
    .isThrownBy(() -> publisher.publish(invalidEvent))
    .withMessageContaining("Serialization failed");
```

---

## 8. 测试报告

### 8.1 JaCoCo配置

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 8.2 覆盖率目标

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>CLASS</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

---

**文档维护**: Redis工具集团队
**更新频率**: 每次规范变更后
**下次审查**: 版本发布前