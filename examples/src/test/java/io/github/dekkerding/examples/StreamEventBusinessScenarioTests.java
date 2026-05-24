package io.github.dekkerding.examples;

import io.github.dekkerding.examples.application.EventPublishingService;
import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.domain.model.ConsumerInfo;
import io.github.dekkerding.examples.infrastructure.publisher.EventPublisher;
import io.github.dekkerding.examples.event.*;
import io.github.dekkerding.examples.infrastructure.consumer.RedisStreamEventConsumer;
import io.github.dekkerding.examples.infrastructure.routing.EventStreamRouter;
import io.github.dekkerding.examples.infrastructure.serialization.EventSerializer;
import io.github.dekkerding.examples.producer.BusinessProcessor;
import io.github.dekkerding.examples.producer.BusinessProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务场景端到端测试：订单、支付、发货、BusinessProcessor桥接
 */
@Slf4j
@SpringBootTest
class StreamEventBusinessScenarioTests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EventPublishingService publishingService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private EventStreamRouter router;

    @Autowired
    private EventSerializer serializer;

    @Autowired
    private RedisStreamEventConsumer eventConsumer;

    @Autowired
    private BusinessProcessorFactory processorFactory;

    @AfterEach
    void cleanup() {
        try {
            redisTemplate.delete("order_stream");
            redisTemplate.delete("payment_stream");
            redisTemplate.delete("shipment_stream");
            redisTemplate.delete("examplesApplication:dlq:order_stream");
            redisTemplate.delete("examplesApplication:dlq:payment_stream");
        } catch (Exception e) {
            log.warn("清理测试数据时出错: {}", e.getMessage());
        }
    }

    // ==================== 订单场景测试 ====================
    @Nested
    @DisplayName("订单场景测试")
    class OrderScenarioTests {

        @Test
        @DisplayName("订单创建事件发布到正确的 Stream")
        void testOrderCreatedEventPublish() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-BIZ-001", "USR-BIZ-001", 1299.99, "iPhone 15,AirPods");

            EventPublisher.MessageId messageId = publishingService.publish(event);
            assertNotNull(messageId);

            // 验证写入了 order_stream
            Long size = redisTemplate.opsForStream().size("order_stream");
            assertEquals(1, size.intValue());
        }

        @Test
        @DisplayName("订单状态变更事件发布")
        void testOrderStatusChangedEventPublish() {
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    this, "ORD-BIZ-002", "CREATED", "PAID");

            EventPublisher.MessageId messageId = publishingService.publish(event);
            assertNotNull(messageId);

            // 同样路由到 order_stream
            Long size = redisTemplate.opsForStream().size("order_stream");
            assertTrue(size > 0);
        }

        @Test
        @DisplayName("订单事件序列化包含完整业务字段")
        void testOrderEventSerializationContent() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-SER-BIZ-001", "USR-SER-001", 2999.0, "MacBook Pro");

            String json = serializer.serialize(event);

            assertTrue(json.contains("ORD-SER-BIZ-001"));
            assertTrue(json.contains("USR-SER-001"));
            assertTrue(json.contains("2999"));
            assertTrue(json.contains("MacBook Pro"));
        }

        @Test
        @DisplayName("订单创建 → 状态变更事件链")
        void testOrderEventChain() {
            // 1. 创建订单
            OrderCreatedEvent createEvent = new OrderCreatedEvent(
                    this, "ORD-CHAIN-001", "USR-CHAIN", 599.0, "商品X");
            publishingService.publish(createEvent);

            // 2. 订单状态变更
            OrderStatusChangedEvent statusEvent = new OrderStatusChangedEvent(
                    this, "ORD-CHAIN-001", "CREATED", "CONFIRMED");
            publishingService.publish(statusEvent);

            // 验证两条消息都在 order_stream 中
            Long size = redisTemplate.opsForStream().size("order_stream");
            assertEquals(2, size.intValue());
        }
    }

    // ==================== 支付场景测试 ====================
    @Nested
    @DisplayName("支付场景测试")
    class PaymentScenarioTests {

        @Test
        @DisplayName("支付完成事件发布到 payment_stream")
        void testPaymentCompletedEventPublish() {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    this, "PAY-001", "ORD-PAY-001", 999.0, "ALIPAY");

            EventPublisher.MessageId messageId = publishingService.publish(event);
            assertNotNull(messageId);

            Long size = redisTemplate.opsForStream().size("payment_stream");
            assertEquals(1, size.intValue());
        }

        @Test
        @DisplayName("支付失败事件发布")
        void testPaymentFailedEventPublish() {
            PaymentFailedEvent event = new PaymentFailedEvent(
                    this, "PAY-FAIL-001", "ORD-PAY-002", "余额不足");

            EventPublisher.MessageId messageId = publishingService.publish(event);
            assertNotNull(messageId);

            Long size = redisTemplate.opsForStream().size("payment_stream");
            assertEquals(1, size.intValue());
        }

        @Test
        @DisplayName("支付事件序列化包含支付方式")
        void testPaymentEventSerialization() {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    this, "PAY-SER-001", "ORD-SER-001", 1500.0, "WECHAT_PAY");

            String json = serializer.serialize(event);
            assertTrue(json.contains("WECHAT_PAY"));
            assertTrue(json.contains("PAY-SER-001"));
        }
    }

    // ==================== BusinessProcessor 桥接测试 ====================
    @Nested
    @DisplayName("BusinessProcessor 桥接测试")
    class BusinessProcessorBridgeTests {

        @Test
        @DisplayName("BusinessProcessorFactory 能获取已注册的处理器类型")
        void testProcessorFactoryRegisteredTypes() {
            // 验证工厂已初始化
            assertNotNull(processorFactory);
            log.info("已注册的消息类型: {}", processorFactory.getSupportedMessageTypes());
        }

        @Test
        @DisplayName("EventEnvelope 可转换为 BusinessProcessor 兼容的 Map")
        void testEnvelopeToProcessorCompatibleMap() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    this, "ORD-BRIDGE-001", "USR-BRIDGE", 100.0, "桥接测试商品");

            String payload = serializer.serialize(event);
            EventEnvelope envelope = EventEnvelope.builder()
                    .metadata(event.getMetadata())
                    .payload(payload)
                    .eventClassName(event.getClass().getName())
                    .build();

            Map<String, String> record = envelope.toStreamRecord();
            record.put("type", event.getEventType());

            // 验证 Map 包含 type 字段
            assertEquals("OrderCreatedEvent", record.get("type"));
            assertNotNull(record.get(EventEnvelope.FIELD_PAYLOAD));
        }
    }

    // ==================== 多事件类型测试 ====================
    @Nested
    @DisplayName("多事件类型测试")
    class MultiEventTypeTests {

        @Test
        @DisplayName("不同事件类型路由到不同 Stream")
        void testMultiEventTypeRouting() {
            // 发布订单事件
            OrderCreatedEvent orderEvent = new OrderCreatedEvent(
                    this, "ORD-MULTI-001", "USR-MULTI", 100.0, "商品A");
            publishingService.publish(orderEvent);

            // 发布支付事件
            PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
                    this, "PAY-MULTI-001", "ORD-MULTI-001", 100.0, "ALIPAY");
            publishingService.publish(paymentEvent);

            // 发布发货事件
            ShipmentCreatedEvent shipmentEvent = new ShipmentCreatedEvent(
                    this, "SHIP-MULTI-001", "ORD-MULTI-001", "北京市朝阳区xxx");
            publishingService.publish(shipmentEvent);

            // 验证各自路由到不同 Stream
            Long orderSize = redisTemplate.opsForStream().size("order_stream");
            Long paymentSize = redisTemplate.opsForStream().size("payment_stream");
            Long shipmentSize = redisTemplate.opsForStream().size("shipment_stream");

            assertEquals(1, orderSize.intValue());
            assertEquals(1, paymentSize.intValue());
            assertEquals(1, shipmentSize.intValue());
        }

        @Test
        @DisplayName("完整业务流程: 下单 → 支付 → 发货")
        void testCompleteBusinessFlow() {
            String orderId = "ORD-FLOW-001";

            // 1. 下单
            OrderCreatedEvent orderEvent = new OrderCreatedEvent(
                    this, orderId, "USR-FLOW", 2999.0, "MacBook Air");
            EventPublisher.MessageId orderRecordId = publishingService.publish(orderEvent);
            assertNotNull(orderRecordId);
            log.info("步骤1 - 订单创建: {}", orderRecordId);

            // 2. 订单确认
            OrderStatusChangedEvent confirmEvent = new OrderStatusChangedEvent(
                    this, orderId, "CREATED", "CONFIRMED");
            EventPublisher.MessageId confirmRecordId = publishingService.publish(confirmEvent);
            assertNotNull(confirmRecordId);
            log.info("步骤2 - 订单确认: {}", confirmRecordId);

            // 3. 支付
            PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
                    this, "PAY-FLOW-001", orderId, 2999.0, "ALIPAY");
            EventPublisher.MessageId paymentRecordId = publishingService.publish(paymentEvent);
            assertNotNull(paymentRecordId);
            log.info("步骤3 - 支付完成: {}", paymentRecordId);

            // 4. 订单状态更新为已支付
            OrderStatusChangedEvent paidEvent = new OrderStatusChangedEvent(
                    this, orderId, "CONFIRMED", "PAID");
            EventPublisher.MessageId paidRecordId = publishingService.publish(paidEvent);
            assertNotNull(paidRecordId);
            log.info("步骤4 - 订单已支付: {}", paidRecordId);

            // 5. 发货
            ShipmentCreatedEvent shipmentEvent = new ShipmentCreatedEvent(
                    this, "SHIP-FLOW-001", orderId, "上海市浦东新区xxx路xxx号");
            EventPublisher.MessageId shipmentRecordId = publishingService.publish(shipmentEvent);
            assertNotNull(shipmentRecordId);
            log.info("步骤5 - 已发货: {}", shipmentRecordId);

            // 验证所有事件都已写入对应 Stream
            Long orderStreamSize = redisTemplate.opsForStream().size("order_stream");
            Long paymentStreamSize = redisTemplate.opsForStream().size("payment_stream");
            Long shipmentStreamSize = redisTemplate.opsForStream().size("shipment_stream");

            assertEquals(3, orderStreamSize.intValue(), "order_stream 应有3条消息(创建+确认+已支付)");
            assertEquals(1, paymentStreamSize.intValue(), "payment_stream 应有1条消息");
            assertEquals(1, shipmentStreamSize.intValue(), "shipment_stream 应有1条消息");

            log.info("✅ 完整业务流程测试通过: 下单→确认→支付→已支付→发货");
        }

        @Test
        @DisplayName("同一 Stream 中多种事件类型共存")
        void testMultipleEventTypesInSameStream() {
            // OrderCreatedEvent 和 OrderStatusChangedEvent 都路由到 order_stream
            OrderCreatedEvent createEvent = new OrderCreatedEvent(
                    this, "ORD-SAME-001", "USR-SAME", 500.0, "商品Y");
            OrderStatusChangedEvent statusEvent = new OrderStatusChangedEvent(
                    this, "ORD-SAME-001", "CREATED", "SHIPPED");

            publishingService.publish(createEvent);
            publishingService.publish(statusEvent);

            Long size = redisTemplate.opsForStream().size("order_stream");
            assertEquals(2, size.intValue(), "同一 Stream 应包含两种不同类型的事件");
        }
    }
}

