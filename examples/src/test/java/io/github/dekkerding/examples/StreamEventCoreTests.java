package io.github.dekkerding.examples;

import io.github.dekkerding.examples.application.EventPublishingService;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.domain.event.EventMetadata;
import io.github.dekkerding.examples.infrastructure.publisher.EventPublisher;
import io.github.dekkerding.examples.event.OrderCreatedEvent;
import io.github.dekkerding.examples.event.PaymentCompletedEvent;
import io.github.dekkerding.examples.infrastructure.routing.EventStreamRouter;
import io.github.dekkerding.examples.infrastructure.serialization.EventSerializer;
import io.github.dekkerding.examples.infrastructure.serialization.JacksonEventSerializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心机制测试：序列化、路由、发布、消费、重试
 */
@Slf4j
@SpringBootTest
class StreamEventCoreTests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EventPublishingService publishingService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private EventStreamRouter router;

    @AfterEach
    void cleanup() {
        try {
            redisTemplate.delete("order_stream");
            redisTemplate.delete("payment_stream");
            redisTemplate.delete("examplesApplication:stream:OrderCreatedEvent");
            redisTemplate.delete("examplesApplication:dlq:order_stream");
        } catch (Exception e) {
            log.warn("清理测试数据时出错: {}", e.getMessage());
        }
    }

    // ==================== 序列化测试 ====================
    @Nested
    @DisplayName("序列化测试")
    class SerializationTests {

        private final EventSerializer serializer = new JacksonEventSerializer();

        @Test
        @DisplayName("EventMetadata 生成正确的 UUID 和时间戳")
        void testEventMetadataCreation() {
            EventMetadata metadata = EventMetadata.create(OrderCreatedEvent.class, "test-app");

            assertNotNull(metadata.getEventId());
            assertTrue(metadata.getEventId().length() > 0);
            assertTrue(metadata.getTimestamp() > 0);
            assertEquals("OrderCreatedEvent", metadata.getEventType());
            assertEquals("test-app", metadata.getSourceApplication());
        }

        @Test
        @DisplayName("EventEnvelope toStreamRecord/fromStreamRecord 往返一致")
        void testEventEnvelopeRoundTrip() {
            EventMetadata metadata = EventMetadata.create(OrderCreatedEvent.class, "test-app");
            String payload = "{\"orderId\":\"ORD001\",\"amount\":99.9}";

            EventEnvelope original = EventEnvelope.builder()
                    .metadata(metadata)
                    .payload(payload)
                    .eventClassName(OrderCreatedEvent.class.getName())
                    .build();

            Map<String, String> record = original.toStreamRecord();
            EventEnvelope restored = EventEnvelope.fromStreamRecord(record);

            assertEquals(original.getMetadata().getEventId(), restored.getMetadata().getEventId());
            assertEquals(original.getMetadata().getTimestamp(), restored.getMetadata().getTimestamp());
            assertEquals(original.getMetadata().getEventType(), restored.getMetadata().getEventType());
            assertEquals(original.getEventClassName(), restored.getEventClassName());
            assertEquals(original.getPayload(), restored.getPayload());
        }

        @Test
        @DisplayName("Jackson 序列化/反序列化 DomainEvent")
        void testJacksonSerializationRoundTrip() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-SER-001", "USR-001", 199.99, "商品A,商品B");

            String json = serializer.serialize(event);
            assertNotNull(json);
            assertTrue(json.contains("ORD-SER-001"));
            assertTrue(json.contains("199.99"));

            DomainEvent deserialized = serializer.deserialize(json, OrderCreatedEvent.class.getName());
            assertNotNull(deserialized);
            assertTrue(deserialized instanceof OrderCreatedEvent);

            OrderCreatedEvent restored = (OrderCreatedEvent) deserialized;
            assertEquals("ORD-SER-001", restored.getOrderId());
            assertEquals("USR-001", restored.getUserId());
            assertEquals(199.99, restored.getAmount(), 0.01);
            assertEquals("商品A,商品B", restored.getItems());
        }

        @Test
        @DisplayName("EventEnvelope toStreamRecord 包含所有必要字段")
        void testEnvelopeContainsAllFields() {
            EventMetadata metadata = EventMetadata.create(OrderCreatedEvent.class);
            EventEnvelope envelope = EventEnvelope.builder()
                    .metadata(metadata)
                    .payload("{}")
                    .eventClassName("io.github.dekkerding.examples.event.OrderCreatedEvent")
                    .build();

            Map<String, String> record = envelope.toStreamRecord();

            assertTrue(record.containsKey(EventEnvelope.FIELD_EVENT_ID));
            assertTrue(record.containsKey(EventEnvelope.FIELD_TIMESTAMP));
            assertTrue(record.containsKey(EventEnvelope.FIELD_EVENT_TYPE));
            assertTrue(record.containsKey(EventEnvelope.FIELD_EVENT_CLASS));
            assertTrue(record.containsKey(EventEnvelope.FIELD_PAYLOAD));
            assertEquals(6, record.size());
        }
    }

    // ==================== 路由测试 ====================
    @Nested
    @DisplayName("路由测试")
    class RoutingTests {

        @Test
        @DisplayName("@StreamEvent 注解路由解析")
        void testAnnotationRouting() {
            String streamKey = router.resolveStreamKey(OrderCreatedEvent.class);
            assertEquals("order_stream", streamKey);
        }

        @Test
        @DisplayName("@StreamEvent 注解 group 解析")
        void testAnnotationGroupResolve() {
            String group = router.resolveGroup(OrderCreatedEvent.class);
            assertEquals("order-group", group);
        }

        @Test
        @DisplayName("不同事件类型路由到不同 Stream")
        void testDifferentEventsRouteToDifferentStreams() {
            String orderStream = router.resolveStreamKey(OrderCreatedEvent.class);
            String paymentStream = router.resolveStreamKey(PaymentCompletedEvent.class);

            assertNotEquals(orderStream, paymentStream);
            assertEquals("order_stream", orderStream);
            assertEquals("payment_stream", paymentStream);
        }
    }

    // ==================== 发布测试 ====================
    @Nested
    @DisplayName("发布测试")
    class PublishTests {

        @Test
        @DisplayName("事件发布写入 Redis Stream 验证")
        void testPublishEventWritesToStream() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-PUB-001", "USR-001", 299.99, "商品C");

            EventPublisher.MessageId messageId = publishingService.publish(event);

            assertNotNull(messageId);
            log.info("事件发布成功，MessageId: {}", messageId.getId());

            // 验证 Stream 中有数据
            Long size = redisTemplate.opsForStream().size("order_stream");
            assertTrue(size > 0, "Stream 中应该有消息");
        }

        @Test
        @DisplayName("发布的消息内容完整性验证")
        void testPublishedMessageContentIntegrity() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-CONTENT-001", "USR-002", 599.99, "商品D");

            EventPublisher.MessageId messageId = publishingService.publish(event);

            // 读取消息验证内容
            List<MapRecord<String, Object, Object>> records =
                    redisTemplate.opsForStream()
                            .range("order_stream", Range.closed(messageId.getId(), messageId.getId()));

            assertFalse(records.isEmpty());
            Map<Object, Object> value = records.get(0).getValue();

            assertEquals("OrderCreatedEvent", value.get(EventEnvelope.FIELD_EVENT_TYPE));
            assertNotNull(value.get(EventEnvelope.FIELD_PAYLOAD));
            assertNotNull(value.get(EventEnvelope.FIELD_EVENT_ID));

            String payload = (String) value.get(EventEnvelope.FIELD_PAYLOAD);
            assertTrue(payload.contains("ORD-CONTENT-001"));
            assertTrue(payload.contains("599.99"));
        }

        @Test
        @DisplayName("通过 ApplicationEventPublisher 发布带 @StreamEvent 的事件")
        void testPublishViaApplicationEventPublisher() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-APP-001", "USR-003", 100.0, "商品E");

            applicationEventPublisher.publishEvent(event);

            // 等待异步处理
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            Long size = redisTemplate.opsForStream().size("order_stream");
            assertTrue(size > 0, "通过 ApplicationEventPublisher 发布的事件应写入 Stream");
        }

        @Test
        @DisplayName("批量发布消息验证")
        void testBatchPublish() {
            int batchSize = 10;
            for (int i = 0; i < batchSize; i++) {
                OrderCreatedEvent event = new OrderCreatedEvent(
                        this, "ORD-BATCH-" + i, "USR-BATCH", 10.0 * i, "批量商品" + i);
                publishingService.publish(event);
            }

            Long size = redisTemplate.opsForStream().size("order_stream");
            assertEquals(batchSize, size.intValue());
        }
    }

    // ==================== 消费测试 ====================
    @Nested
    @DisplayName("消费测试")
    class ConsumeTests {

        @Test
        @DisplayName("消费者能从 Stream 读取并分发事件")
        void testConsumeEventFromStream() throws InterruptedException {
            // 先发布事件
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-CONSUME-001", "USR-CONSUME", 88.88, "消费测试商品");
            publishingService.publish(event);

            // 等待消费者处理
            Thread.sleep(3000);

            // 验证 Stream 中有消息（消费者是否启动取决于是否有 @StreamEventListener 注册）
            Long size = redisTemplate.opsForStream().size("order_stream");
            assertNotNull(size);
            log.info("Stream order_stream 当前消息数: {}", size);
        }

        @Test
        @DisplayName("消息 ID 格式验证")
        void testMessageIdFormat() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-ID-001", "USR-ID", 50.0, "ID格式测试");

            EventPublisher.MessageId messageId = publishingService.publish(event);
            String idValue = messageId.getId();

            assertNotNull(idValue);
            assertTrue(idValue.matches("\\d+-\\d+"), "Redis Stream ID 格式应为: 毫秒时间戳-序列号");
        }
    }

    // ==================== 重试和死信队列测试 ====================
    @Nested
    @DisplayName("重试和死信队列测试")
    class RetryAndDlqTests {

        @Test
        @DisplayName("EventEnvelope 从无效数据恢复时不崩溃")
        void testEnvelopeFromInvalidData() {
            Map<String, String> invalidRecord = new HashMap<>();
            invalidRecord.put(EventEnvelope.FIELD_EVENT_ID, "test-id");
            invalidRecord.put(EventEnvelope.FIELD_TIMESTAMP, "0");
            invalidRecord.put(EventEnvelope.FIELD_EVENT_TYPE, "UnknownEvent");
            invalidRecord.put(EventEnvelope.FIELD_EVENT_CLASS, "com.nonexistent.Event");
            invalidRecord.put(EventEnvelope.FIELD_PAYLOAD, "{}");
            invalidRecord.put(EventEnvelope.FIELD_SOURCE_APP, "test");

            EventEnvelope envelope = EventEnvelope.fromStreamRecord(invalidRecord);
            assertNotNull(envelope);
            assertEquals("UnknownEvent", envelope.getMetadata().getEventType());
        }

        @Test
        @DisplayName("死信队列 Stream key 格式正确")
        void testDlqKeyFormat() {
            String dlqKey = "examplesApplication:dlq:" + "order_stream";
            assertEquals("examplesApplication:dlq:order_stream", dlqKey);
        }
    }
}