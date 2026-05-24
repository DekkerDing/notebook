# Redis Stream 到 Kafka 迁移部署指南

## 环境准备

### 1. Kafka 环境部署

#### 方案A: Docker 单节点部署 (开发/测试环境)

```bash
# 启动 Zookeeper
docker run -d --name zookeeper \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  confluentinc/cp-zookeeper:latest

# 启动 Kafka
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.10.109:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  --link zookeeper:zookeeper \
  confluentinc/cp-kafka:latest
```

#### 方案B: 本地安装

```bash
# 下载 Kafka
wget https://downloads.apache.org/kafka/3.2.3/kafka_2.13-3.2.3.tgz
tar -xzf kafka_2.13-3.2.3.tgz
cd kafka_2.13-3.2.3

# 启动 Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# 启动 Kafka
bin/kafka-server-start.sh config/server.properties
```

#### 方案C: 使用现有 Redis 集群服务器部署

在 `192.168.10.107` 或 `192.168.10.109` 上部署 Kafka:

```bash
# 修改 server.properties
listeners=PLAINTEXT://192.168.10.109:9092
advertised.listeners=PLAINTEXT://192.168.10.109:9092
zookeeper.connect=192.168.10.109:2181
```

### 2. 验证 Kafka 环境

```bash
# 创建测试 Topic
bin/kafka-topics.sh --create \
  --topic test-topic \
  --bootstrap-server 192.168.10.109:9092 \
  --partitions 3 \
  --replication-factor 1

# 列出 Topics
bin/kafka-topics.sh --list \
  --bootstrap-server 192.168.10.109:9092

# 测试生产者
bin/kafka-console-producer.sh \
  --topic test-topic \
  --bootstrap-server 192.168.10.109:9092

# 测试消费者
bin/kafka-console-consumer.sh \
  --topic test-topic \
  --bootstrap-server 192.168.10.109:9092 \
  --from-beginning
```

---

## 配置切换

### 方式1: 完全切换到 Kafka (推荐用于新环境)

```yaml
# application.yaml
spring:
  redis:
    stream:
      event:
        enabled: false  # 禁用 Redis Stream
  kafka:
    event:
      enabled: true    # 启用 Kafka
      consumer:
        bootstrap-servers: 192.168.10.109:9092
      producer:
        bootstrap-servers: 192.168.10.109:9092
```

### 方式2: 灰度切换 (部分事件使用 Kafka)

```yaml
spring:
  kafka:
    event:
      enabled: true
      routes:
        OrderCreatedEvent: order-topic        # Kafka
        PaymentCompletedEvent: payment-topic  # Kafka
        # 其他事件继续使用 Redis Stream
  redis:
    stream:
      event:
        enabled: true
```

### 方式3: 环境变量切换

```bash
# 启用 Kafka
export SPRING_KAFKA_EVENT_ENABLED=true
export SPRING_REDIS_STREAM_EVENT_ENABLED=false

# 启用 Redis Stream (默认)
export SPRING_KAFKA_EVENT_ENABLED=false
export SPRING_REDIS_STREAM_EVENT_ENABLED=true
```

---

## Redis 资源清理

### 清理 Redis Stream 数据

在切换到 Kafka 后，可以清理 Redis 中的 Stream 数据:

```bash
# 连接到 Redis 集群
redis-cli -c -h 192.168.10.107 -p 6379 -a drk@2025

# 1. 查看所有 Stream
redis-cli> KEYS examplesApplication:stream:*

# 2. 查看 Stream 长度
redis-cli> XLEN examplesApplication:stream:order_stream

# 3. 清空指定 Stream
redis-cli> DEL examplesApplication:stream:order_stream

# 4. 批量清空所有 Stream (谨慎使用)
redis-cli> --scan --pattern examplesApplication:stream:* | xargs redis-cli DEL

# 5. 清理 DLQ
redis-cli> --scan --pattern examplesApplication:dlq:* | xargs redis-cli DEL
```

### 清理消费者组

```bash
# 查看消费者组
redis-cli> XINFO GROUPS examplesApplication:stream:order_stream

# 删除消费者组
redis-cli> XGROUP DESTROY examplesApplication:stream:order_stream default-group

# 批量删除所有 Stream 的消费者组
redis-cli> for key in $(redis-cli --scan --pattern examplesApplication:stream:*); do
            redis-cli XGROUP DESTROY $key default-group
          done
```

### 清理脚本

创建清理脚本 `cleanup_redis_streams.sh`:

```bash
#!/bin/bash
REDIS_HOST="192.168.10.107"
REDIS_PORT="6379"
REDIS_PASSWORD="drk@2025"
KEY_PREFIX="examplesApplication:stream:"
DLQ_PREFIX="examplesApplication:dlq:"

echo "清理 Redis Stream 数据..."
redis-cli -c -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD \
  --scan --pattern "${KEY_PREFIX}*" | \
  xargs -I {} redis-cli -c -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD DEL {}

echo "清理 DLQ 数据..."
redis-cli -c -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD \
  --scan --pattern "${DLQ_PREFIX}*" | \
  xargs -I {} redis-cli -c -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD DEL {}

echo "清理完成!"
```

---

## 应用部署

### 1. 构建应用

```bash
cd examples
./gradlew clean build -x test
```

### 2. 启动应用 (Kafka 模式)

```bash
java -jar build/libs/examples-1.0-SNAPSHOT.jar \
  --spring.profiles.active=kafka \
  --spring.kafka.event.enabled=true \
  --spring.redis.stream.event.enabled=false
```

### 3. 启动应用 (Redis Stream 模式)

```bash
java -jar build/libs/examples-1.0-SNAPSHOT.jar \
  --spring.profiles.active=default \
  --spring.kafka.event.enabled=false \
  --spring.redis.stream.event.enabled=true
```

---

## 监控和验证

### 1. 检查 Kafka Topic

```bash
# 列出所有 Topic
bin/kafka-topics.sh --list --bootstrap-server 192.168.10.109:9092

# 查看 Topic 详情
bin/kafka-topics.sh --describe \
  --topic examplesApplication.OrderCreatedEvent \
  --bootstrap-server 192.168.10.109:9092

# 消费消息验证
bin/kafka-console-consumer.sh \
  --topic examplesApplication.OrderCreatedEvent \
  --bootstrap-server 192.168.10.109:9092 \
  --from-beginning
```

### 2. 检查应用健康状态

```bash
# 检查健康端点
curl http://localhost:8080/actuator/health

# 检查 Kafka 消费者状态
curl http://localhost:8080/actuator/health/kafkaConsumer
```

### 3. 日志监控

```bash
# 查看应用日志
tail -f logs/application.log | grep -E "📤|📨|✅|❌"

# 只看 Kafka 相关日志
tail -f logs/application.log | grep -i kafka
```

---

## 回滚策略

如果 Kafka 迁移出现问题，可以快速回滚到 Redis Stream:

```yaml
# 立即回滚配置
spring:
  kafka:
    event:
      enabled: false
  redis:
    stream:
      event:
        enabled: true
```

重启应用后，系统将使用 Redis Stream。

---

## 性能调优

### Kafka 生产者优化

```yaml
spring:
  kafka:
    event:
      producer:
        # 批量发送大小
        batch-size: 16384
        # 缓冲区大小
        buffer-memory: 33554432
        # 压缩类型
        compression-type: snappy
```

### Kafka 消费者优化

```yaml
spring:
  kafka:
    event:
      consumer:
        # 会话超时
        session-timeout-ms: 30000
        # 心跳间隔
        heartbeat-interval-ms: 10000
        # 最大轮询间隔
        max-poll-interval-ms: 300000
```

---

## 故障排查

### 常见问题

1. **连接失败**: `Connection to node -1 could not be established`
   - 检查 `bootstrap-servers` 配置
   - 验证网络连通性: `telnet 192.168.10.109 9092`

2. **消费者组异常**: `The group is already rebalancing`
   - 增加会话超时时间
   - 减少并发数量

3. **消息堆积**: Consumer lag 持续增长
   - 增加消费者并发数
   - 优化消息处理逻辑
   - 检查是否有消费失败导致重试

---

## 资源链接

- [Spring Kafka 文档](https://docs.spring.io/spring-kafka/reference/)
- [Apache Kafka 文档](https://kafka.apache.org/documentation/)
- 项目路径: `F:\workspace\notebook\examples`
- Redis 集群: `192.168.10.107:6379, 192.168.10.109:6379`
- Kafka 集群: `192.168.10.109:9092` (待部署)
