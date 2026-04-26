package io.github.dekkerding.examples;

import io.github.dekkerding.examples.manager.DynamicStreamManager;
import io.github.dekkerding.examples.producer.BusinessProcessor;
import io.github.dekkerding.examples.producer.BusinessProcessorFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
@Testcontainers
class ExamplesApplicationTests {

    private static final String STREAM_KEY = "test_stream";
    private static final String GROUP = "test_group";
    private static final String CONSUMER = "test_consumer";

    private static final String CONSUMER_STREAM_KEY = "consumer_test_stream";
    private static final String CONSUMER_GROUP = "consumer_test_group";

    private static final String PERF_STREAM_KEY = "perf_test_stream";
    private static final String PERF_GROUP = "perf_test_group";
    private static final String PERF_CONSUMER = "perf_test_consumer";

    String INTEGRATION_STREAM_KEY = "integration_test_stream";
    String INTEGRATION_GROUP = "integration_test_group";
    String INTEGRATION_CONSUMER = "integration_test_consumer";

    private static final String PRODUCER_STREAM_KEY = "producer_test_stream";
    private static final String PRODUCER_GROUP = "producer_test_group";
    private static final String PRODUCER_CONSUMER = "producer_test_consumer";

    // 启动多个消费者
    String consumer1 = "consumer-1";
    String consumer2 = "consumer-2";

    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    @Autowired
    protected DynamicStreamManager streamManager;

    @Autowired
    protected BusinessProcessorFactory processorFactory;

//    @Mock
//    private OrderService orderService;

    private OrderBusinessProcessor orderProcessor;

    @Test
    @DisplayName("测试发送简单消息")
    public void testSendSimpleMessage() throws InterruptedException {

        // ✅ 确保消费者组存在
        ensureConsumerGroupExists();

        // 发送消息
        Map<String, String> message = new HashMap<>();
        message.put("type", "TEST");
        message.put("content", "Hello Redis Stream");

        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(message));

        Assertions.assertNotNull(recordId);
        log.info("发送消息成功，ID: {}", recordId.getValue());

        // 等待消费者处理
        Thread.sleep(1000);

        // 验证消息已被确认
        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(STREAM_KEY, GROUP);
        Assertions.assertEquals(0, pending.getTotalPendingMessages());
    }

    private void ensureConsumerGroupExists() {
        try {
            // 尝试创建消费者组
            redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
            log.info("消费者组已创建: {} -> {}", STREAM_KEY, GROUP);
        } catch (Exception e) {
            // 如果消费者组已存在，忽略异常
            if (e.getMessage().contains("BUSYGROUP")) {
                log.info("消费者组已存在: {} -> {}", STREAM_KEY, GROUP);
            } else {
                log.error("创建消费者组失败", e);
                throw e;
            }
        }
    }


    @Test
    @DisplayName("测试发送订单创建消息")
    public void testSendOrderCreateMessage() throws InterruptedException {
        Map<String, String> message = new HashMap<>();
        message.put("type", "ORDER");
        message.put("action", "CREATE");
        message.put("orderId", "ORD-PRODUCER-TEST-001");
        message.put("userId", "USR-PRODUCER-TEST-001");
        message.put("amount", "299.99");

        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .in(PRODUCER_STREAM_KEY)
                        .ofMap(message));

        Assertions.assertNotNull(recordId);
        log.info("发送订单创建消息成功，ID: {}", recordId.getValue());

        Thread.sleep(1000);

        // 验证消息已被处理
        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(PRODUCER_STREAM_KEY, PRODUCER_GROUP);
        Assertions.assertEquals(0, pending.getTotalPendingMessages());
    }

    @Test
    @DisplayName("测试发送无效消息类型")
    public void testSendInvalidMessageType() throws InterruptedException {
        Map<String, String> message = new HashMap<>();
        message.put("type", "INVALID_TYPE");
        message.put("action", "CREATE");

        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .in(PRODUCER_STREAM_KEY)
                        .ofMap(message));

        Assertions.assertNotNull(recordId);
        log.info("发送无效类型消息，ID: {}", recordId.getValue());

        Thread.sleep(2000); // 等待重试

        // 验证消息进入死信队列
        String dlqKey = "dlq:" + PRODUCER_STREAM_KEY;
        Long dlqSize = redisTemplate.opsForList().size(dlqKey);
        Assertions.assertTrue(dlqSize > 0);
    }

    @Test
    @DisplayName("测试发送批量消息")
    public void testSendBatchMessages() throws InterruptedException {
        int batchSize = 10;

        for (int i = 0; i < batchSize; i++) {
            Map<String, String> message = new HashMap<>();
            message.put("type", "BATCH_TEST");
            message.put("index", String.valueOf(i));
            message.put("timestamp", String.valueOf(System.currentTimeMillis()));

            RecordId recordId = redisTemplate.opsForStream()
                    .add(StreamRecords.newRecord()
                            .in(PRODUCER_STREAM_KEY)
                            .ofMap(message));

            Assertions.assertNotNull(recordId);
        }

        log.info("批量发送 {} 条消息完成", batchSize);

        // 等待所有消息处理完成
        Thread.sleep(3000);

        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(PRODUCER_STREAM_KEY, PRODUCER_GROUP);
        Assertions.assertEquals(0, pending.getTotalPendingMessages());
    }

    @Test
    @DisplayName("测试消息ID格式")
    public void testMessageIdFormat() {
        Map<String, String> message = new HashMap<>();
        message.put("type", "ID_FORMAT_TEST");
        message.put("content", "测试消息ID格式");

        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .in(PRODUCER_STREAM_KEY)
                        .ofMap(message));

        String idValue = recordId.getValue();
        Assertions.assertNotNull(idValue);

        // Redis Stream ID 格式：毫秒时间戳-序列号
        Assertions.assertTrue(idValue.matches("\\d+-\\d+"));

        log.info("消息ID格式正确: {}", idValue);
    }

    @Test
    @DisplayName("测试消息内容完整性")
    public void testMessageContentIntegrity() throws InterruptedException {
        Map<String, String> originalMessage = new HashMap<>();
        originalMessage.put("type", "CONTENT_TEST");
        originalMessage.put("field1", "value1");
        originalMessage.put("field2", "value2");
        originalMessage.put("number", "123");

        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .in(PRODUCER_STREAM_KEY)
                        .ofMap(originalMessage));

        // 读取消息验证内容
        List<MapRecord<String, Object, Object>> records =
                redisTemplate.opsForStream()
                        .range(PRODUCER_STREAM_KEY, Range.closed(recordId.getValue(), recordId.getValue()));

        Assertions.assertFalse(records.isEmpty());
        MapRecord<String, Object, Object> savedRecord = records.get(0);
        Map<Object, Object> savedValue = savedRecord.getValue();

        Assertions.assertEquals("CONTENT_TEST", savedValue.get("type"));
        Assertions.assertEquals("value1", savedValue.get("field1"));
        Assertions.assertEquals("value2", savedValue.get("field2"));
        Assertions.assertEquals("123", savedValue.get("number"));

        Thread.sleep(1000);
    }

    @Test
    @DisplayName("测试启动消费者")
    public void testStartConsumer() {
        // 启动消费者
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 验证消费者已启动
        List<String> activeConsumers = streamManager.getActiveConsumers();
        Assertions.assertTrue(activeConsumers.contains(STREAM_KEY + ":" + GROUP + ":" + CONSUMER));
    }

    @Test
    @DisplayName("测试重复启动消费者")
    public void testStartDuplicateConsumer() {
        // 第一次启动
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 第二次启动（应该被阻止）
        // streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 验证只有一个消费者
        List<String> activeConsumers = streamManager.getActiveConsumers();
        Assertions.assertEquals(1, activeConsumers.size());
    }

    @Test
    @DisplayName("测试停止消费者")
    public void testStopConsumer() {
        // 启动消费者
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 停止消费者
        streamManager.stopConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 验证消费者已停止
        List<String> activeConsumers = streamManager.getActiveConsumers();
        Assertions.assertFalse(activeConsumers.contains(STREAM_KEY + ":" + GROUP + ":" + CONSUMER));
    }

    @Test
    @DisplayName("测试暂停和恢复消费者")
    public void testPauseAndResumeConsumer() {
        // 启动消费者
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 暂停消费者
        streamManager.pauseConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 验证消费者已暂停
        List<String> activeConsumers = streamManager.getActiveConsumers();
        Assertions.assertFalse(activeConsumers.contains(STREAM_KEY + ":" + GROUP + ":" + CONSUMER));

        // 恢复消费者
        streamManager.resumeConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 验证消费者已恢复
        activeConsumers = streamManager.getActiveConsumers();
        Assertions.assertTrue(activeConsumers.contains(STREAM_KEY + ":" + GROUP + ":" + CONSUMER));
    }

    @Test
    @DisplayName("测试批量启动消费者组")
    public void testStartConsumerGroup() {
        int consumerCount = 3;
        streamManager.startConsumerGroup(STREAM_KEY, GROUP, consumerCount);

        // 验证消费者组已启动
        List<String> activeConsumers = streamManager.getActiveConsumers();
        Assertions.assertEquals(consumerCount, activeConsumers.size());
    }

    @Test
    @DisplayName("测试消息处理成功")
    public void testSuccessfulMessageProcessing() throws InterruptedException {
        // 启动消费者
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 发送测试消息
        Map<String, String> message = new HashMap<>();
        message.put("type", "ORDER");
        message.put("action", "CREATE");
        message.put("orderId", "ORD123");

        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(message)
        );

        // 等待消息处理
        Thread.sleep(1000);

        // 验证消息已被确认（从待处理列表中移除）
        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(STREAM_KEY, GROUP);
        Assertions.assertEquals(0, pending.getTotalPendingMessages());
    }

    @Test
    @DisplayName("测试消息处理失败重试")
    public void testMessageRetry() throws InterruptedException {
        // 启动消费者
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 发送会失败的消息
        Map<String, String> message = new HashMap<>();
        message.put("type", "INVALID");
        message.put("action", "FAIL");

        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(message)
        );

        // 等待重试
        Thread.sleep(5000);

        // 验证消息进入死信队列
        String dlqKey = "dlq:" + STREAM_KEY;
        Long dlqSize = redisTemplate.opsForList().size(dlqKey);
        Assertions.assertTrue(dlqSize > 0);
    }

    @Test
    @DisplayName("测试消息确认机制")
    public void testMessageAcknowledgment() throws InterruptedException {
        // 启动消费者
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 发送消息
        Map<String, String> message = new HashMap<>();
        message.put("type", "ORDER");
        message.put("action", "CREATE");

        RecordId recordId = redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(message)
        );

        // 等待消息处理
        Thread.sleep(1000);

        // 验证消息已被确认
        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(STREAM_KEY, GROUP);
        Assertions.assertEquals(0, pending.getTotalPendingMessages());
    }

    @Test
    @DisplayName("测试订单创建处理")
    public void testOrderCreation() {
        // 准备测试数据
        Map<String, String> message = new HashMap<>();
        message.put("type", "ORDER");
        message.put("action", "CREATE");
        message.put("orderId", "ORD123");
        message.put("userId", "USR456");
        message.put("amount", "100.50");

        // 执行处理
        boolean result = orderProcessor.process(message);

        // 验证结果
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("测试无效消息类型")
    public void testInvalidMessageType() {
        Map<String, String> message = new HashMap<>();
        message.put("type", "INVALID");
        message.put("action", "CREATE");

        boolean result = orderProcessor.process(message);
        Assertions.assertFalse(result);
    }

    @Test
    @DisplayName("测试缺失必需字段")
    public void testMissingRequiredFields() {
        Map<String, String> message = new HashMap<>();
        message.put("type", "ORDER");
        // 缺少 orderId

        boolean result = orderProcessor.process(message);
        Assertions.assertFalse(result);
    }

    @Test
    @DisplayName("测试创建Stream和消费者组")
    public void testCreateStreamAndGroup() {
        // 验证Stream不存在
        Boolean exists = redisTemplate.hasKey(STREAM_KEY);
        Assertions.assertFalse(exists);

        // 启动消费者（会自动创建Stream和组）
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 验证Stream已创建
        exists = redisTemplate.hasKey(STREAM_KEY);
        Assertions.assertTrue(exists);
    }

    @Test
    @DisplayName("测试读取Stream信息")
    public void testReadStreamInfo() {
        // 启动消费者
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 发送消息
        Map<String, String> message = new HashMap<>();
        message.put("type", "TEST");
        message.put("data", "test_data");

        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(message)
        );

        // 验证Stream长度
        Long length = redisTemplate.opsForStream().size(STREAM_KEY);
        Assertions.assertEquals(1, length.longValue());
    }

    @Test
    @DisplayName("测试删除Stream")
    public void testDeleteStream() {
        // 创建Stream
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 删除Stream
        redisTemplate.delete(STREAM_KEY);

        // 验证Stream已删除
        Boolean exists = redisTemplate.hasKey(STREAM_KEY);
        Assertions.assertFalse(exists);
    }

    @Test
    @DisplayName("测试修剪Stream")
    public void testTrimStream() {
        // 启动消费者
        streamManager.startConsumer(STREAM_KEY, GROUP, CONSUMER);

        // 发送多条消息
        for (int i = 0; i < 10; i++) {
            Map<String, String> message = new HashMap<>();
            message.put("type", "TEST");
            message.put("index", String.valueOf(i));

            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(STREAM_KEY)
                            .ofMap(message)
            );
        }

        // 修剪Stream，保留最近5条
        Long trimmed = redisTemplate.opsForStream().trim(STREAM_KEY, 5);
        Assertions.assertEquals(5, trimmed.longValue());

        // 验证Stream长度
        Long length = redisTemplate.opsForStream().size(STREAM_KEY);
        Assertions.assertEquals(5, length.longValue());
    }

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("测试消费者故障转移")
    public void testConsumerFailover() throws InterruptedException {
        // 启动两个消费者
        String consumer1 = "consumer-1";
        String consumer2 = "consumer-2";

        streamManager.startConsumer(CONSUMER_STREAM_KEY, CONSUMER_GROUP, consumer1);
        streamManager.startConsumer(CONSUMER_STREAM_KEY, CONSUMER_GROUP, consumer2);

        // 发送消息
        Map<String, String> message = new HashMap<>();
        message.put("type", "TEST");
        message.put("data", "failover_test");

        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(CONSUMER_STREAM_KEY)
                        .ofMap(message)
        );

        // 停止第一个消费者
        streamManager.stopConsumer(CONSUMER_STREAM_KEY, CONSUMER_GROUP, consumer1);

        // 等待第二个消费者处理消息
        Thread.sleep(1000);

        // 验证消息已被处理
        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(CONSUMER_STREAM_KEY, CONSUMER_GROUP);
        Assertions.assertEquals(0, pending.getTotalPendingMessages());
    }

    @Test
    @DisplayName("测试消息吞吐量")
    public void testThroughput() throws InterruptedException {
        // 启动消费者
        streamManager.startConsumer(PERF_STREAM_KEY, PERF_GROUP, PERF_CONSUMER);

        int messageCount = 1000;
        long startTime = System.currentTimeMillis();

        // 发送大量消息
        for (int i = 0; i < messageCount; i++) {
            Map<String, String> message = new HashMap<>();
            message.put("type", "PERF_TEST");
            message.put("index", String.valueOf(i));
            message.put("timestamp", String.valueOf(System.currentTimeMillis()));

            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(STREAM_KEY)
                            .ofMap(message)
            );
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double throughput = (double) messageCount / (duration / 1000.0);

        log.info("发送 {} 条消息耗时 {} ms，吞吐量: {}/秒",
                messageCount, duration, throughput);

        // 等待所有消息处理完成
        Thread.sleep(5000);

        // 验证所有消息都被处理
        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(PERF_STREAM_KEY, PERF_GROUP);
        Assertions.assertEquals(0, pending.getTotalPendingMessages());
    }

    @Test
    @DisplayName("测试消费者负载均衡")
    public void testLoadBalancing() throws InterruptedException {
        int consumerCount = 3;
        int messagesPerConsumer = 100;

        // 启动多个消费者
        for (int i = 1; i <= consumerCount; i++) {
            streamManager.startConsumer(
                    PERF_STREAM_KEY, PERF_GROUP, "consumer-" + i);
        }

        // 发送消息
        for (int i = 0; i < consumerCount * messagesPerConsumer; i++) {
            Map<String, String> message = new HashMap<>();
            message.put("type", "LOAD_BALANCE_TEST");
            message.put("index", String.valueOf(i));

            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(PERF_STREAM_KEY)
                            .ofMap(message)
            );
        }

        // 等待处理完成
        Thread.sleep(10000);

        // 验证所有消息都被处理
        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(PERF_STREAM_KEY, PERF_GROUP);
        Assertions.assertEquals(0, pending.getGroupName());
    }

    @Test
    @DisplayName("完整业务流程测试")
    public void testCompleteWorkflow() throws InterruptedException {
        // 1. 启动消费者
        streamManager.startConsumer(INTEGRATION_STREAM_KEY, INTEGRATION_GROUP, INTEGRATION_CONSUMER);

        // 2. 发送订单创建消息
        Map<String, String> orderMessage = new HashMap<>();
        orderMessage.put("type", "ORDER");
        orderMessage.put("action", "CREATE");
        orderMessage.put("orderId", "INTEGRATION_ORD001");
        orderMessage.put("userId", "INTEGRATION_USR001");
        orderMessage.put("amount", "199.99");

        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(INTEGRATION_STREAM_KEY)
                        .ofMap(orderMessage)
        );
        // 5. 等待处理完成
        Thread.sleep(3000);

        // 6. 验证所有消息都被处理
        PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(INTEGRATION_STREAM_KEY, INTEGRATION_GROUP);
        Assertions.assertEquals(0, pending.getTotalPendingMessages());

        // 7. 验证死信队列为空
        String dlqKey = "dlq:" + INTEGRATION_STREAM_KEY;
        Long dlqSize = redisTemplate.opsForList().size(dlqKey);
        Assertions.assertEquals(0, dlqSize.longValue());
    }

    @BeforeEach
    public void setUp() {

    }

    @AfterEach
    public void tearDown() {
        // 停止所有消费者
        List<String> activeConsumers = streamManager.getActiveConsumers();
        for (String subscriptionId : activeConsumers) {
            String[] parts = subscriptionId.split(":");
            if (parts.length == 3) {
                streamManager.stopConsumer(parts[0], parts[1], parts[2]);
            }
        }
        // 清理 Redis 数据
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Service
    public class OrderService {
        @Autowired
        private StringRedisTemplate redisTemplate;

        /**
         * 发送订单消息
         */
        public RecordId sendOrderMessage(Order order) {
            Map<String, String> message = new HashMap<>();
            message.put("orderId", order.getId());
            message.put("userId", order.getUserId());
            message.put("amount", String.valueOf(order.getAmount()));
            message.put("status", "CREATED");
            message.put("timestamp", String.valueOf(System.currentTimeMillis()));

            RecordId recordId = redisTemplate.opsForStream()
                    .add(StreamRecords.newRecord()
                            .in("order_stream")
                            .ofMap(message));

            log.info("订单消息已发送: ID={}, Order={}", recordId, order);
            return recordId;
        }

        @Data
        public class Order {
            private String id;
            private String userId;
            private double amount;
        }
    }
    @Component("ORDER")   // ← 这里指定 Bean Name = 消息类型
    public class OrderBusinessProcessor implements BusinessProcessor {

        private final OrderService orderService;

        public OrderBusinessProcessor(OrderService orderService) {
            this.orderService = orderService;
        }

        @Override
        public boolean process(Map<String, String> message) {
            log.info("处理订单消息: {}", message);
            return true;
        }

        /**
         * 获取支持的消息类型
         */
        @Override
        public String getSupportedMessageType() {
            return "ORDER";
        }
    }
}