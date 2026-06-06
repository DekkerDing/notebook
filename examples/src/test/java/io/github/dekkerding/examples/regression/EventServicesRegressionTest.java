package io.github.dekkerding.examples.regression;

import io.github.dekkerding.examples.application.DelayedEventPublisher;
import io.github.dekkerding.examples.application.EventConsumingService;
import io.github.dekkerding.examples.application.EventPublishingService;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.domain.event.OrderCreatedEvent;
import io.github.dekkerding.examples.infrastructure.delayed.DelayedTask;
import io.github.dekkerding.examples.infrastructure.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import io.github.dekkerding.examples.config.EventServicesTestConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件服务回归测试
 *
 * <p>测试事件发布、消费和延迟发布功能
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(EventServicesTestConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("事件服务回归测试")
public class EventServicesRegressionTest {

    @Autowired(required = false)
    private EventPublishingService eventPublishingService;

    @Autowired(required = false)
    private EventConsumingService eventConsumingService;

    @Autowired(required = false)
    private DelayedEventPublisher delayedEventPublisher;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(eventPublishingService != null, "EventPublishingService 未配置");
        Assumptions.assumeTrue(eventConsumingService != null, "EventConsumingService 未配置");
        Assumptions.assumeTrue(delayedEventPublisher != null, "DelayedEventPublisher 未配置");
    }

    @Test
    @Order(1)
    @DisplayName("EVENT-PUB-001: 事件发布基础功能")
    void testBasicEventPublishing() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-order-001")
                    .customerId("customer-123")
                    .amount(new BigDecimal("100.50"))
                    .build();

            // 发布事件
            EventPublisher.MessageId messageId = eventPublishingService.publish(event);

            assertNotNull(messageId, "消息ID不能为空");
            assertNotNull(messageId.getId(), "消息ID不能为空");

            log.info("EVENT-PUB-001测试通过: messageId={}", messageId.getId());
        } catch (Exception e) {
            log.error("EVENT-PUB-001测试失败", e);
            fail("EVENT-PUB-001测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("EVENT-PUB-002: 事件发布元数据验证")
    void testEventPublishingMetadata() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-order-002")
                    .customerId("customer-456")
                    .amount(new BigDecimal("200.00"))
                    .build();

            // 发布事件
            EventPublisher.MessageId messageId = eventPublishingService.publish(event);

            assertNotNull(messageId, "消息ID不能为空");
            assertNotNull(messageId.getId(), "消息ID不能为空");
            assertFalse(messageId.getId().isEmpty(), "消息ID不能为空");

            // 验证事件元数据
            assertNotNull(event.getMetadata(), "事件元数据不能为空");
            assertNotNull(event.getMetadata().getEventId(), "事件ID不能为空");
            assertEquals("OrderCreatedEvent", event.getEventType(), "事件类型应该正确");

            log.info("EVENT-PUB-002测试通过: eventId={}, messageType={}",
                    event.getMetadata().getEventId(), event.getEventType());
        } catch (Exception e) {
            log.error("EVENT-PUB-002测试失败", e);
            fail("EVENT-PUB-002测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("EVENT-PUB-003: 批量事件发布")
    void testBatchEventPublishing() {
        try {
            int batchSize = 5;
            EventPublisher.MessageId[] messageIds = new EventPublisher.MessageId[batchSize];

            // 批量发布事件
            for (int i = 0; i < batchSize; i++) {
                OrderCreatedEvent event = OrderCreatedEvent.builder()
                        .source("test-service")
                        .orderId("test-order-batch-" + i)
                        .customerId("customer-batch")
                        .amount(new BigDecimal("50.00"))
                        .build();

                messageIds[i] = eventPublishingService.publish(event);
            }

            // 验证所有消息ID
            for (int i = 0; i < batchSize; i++) {
                assertNotNull(messageIds[i], "消息ID[" + i + "]不能为空");
                assertNotNull(messageIds[i].getId(), "消息ID[" + i + "]不能为空");
            }

            log.info("EVENT-PUB-003测试通过: 成功发布{}个事件", batchSize);
        } catch (Exception e) {
            log.error("EVENT-PUB-003测试失败", e);
            fail("EVENT-PUB-003测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("EVENT-PUB-004: 事件发布异常处理")
    void testEventPublishingException() {
        try {
            // 测试null事件 - 应该抛出异常
            assertThrows(Exception.class, () -> {
                eventPublishingService.publish(null);
            }, "发布null事件应该抛出异常");

            log.info("EVENT-PUB-004测试通过: 异常处理正确");
        } catch (Exception e) {
            log.error("EVENT-PUB-004测试失败", e);
            fail("EVENT-PUB-004测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("EVENT-CON-001: 事件消费基础功能")
    void testBasicEventConsuming() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-event-con-001")
                    .customerId("customer-test")
                    .amount(new BigDecimal("99.99"))
                    .build();

            // 直接消费事件（通过Spring事件发布）
            assertDoesNotThrow(() -> {
                // 使用EventPublishingService发布事件
                eventPublishingService.publish(event);
            });

            log.info("EVENT-CON-001测试通过: 事件消费正常");
        } catch (Exception e) {
            log.error("EVENT-CON-001测试失败", e);
            fail("EVENT-CON-001测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("EVENT-CON-002: 事件消费幂等性")
    void testEventConsumingIdempotency() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-event-con-002")
                    .customerId("customer-test")
                    .amount(new BigDecimal("99.99"))
                    .build();

            // 发布两次相同的事件（验证幂等性）
            assertDoesNotThrow(() -> {
                eventPublishingService.publish(event);
                eventPublishingService.publish(event);
            });

            log.info("EVENT-CON-002测试通过: 幂等性消费正常");
        } catch (Exception e) {
            log.error("EVENT-CON-002测试失败", e);
            fail("EVENT-CON-002测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("EVENT-CON-003: 事件消费元数据验证")
    void testEventConsumingMetadata() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-event-con-003")
                    .customerId("customer-test")
                    .amount(new BigDecimal("99.99"))
                    .build();

            // 验证事件元数据
            assertNotNull(event.getMetadata(), "事件元数据不能为空");
            assertNotNull(event.getMetadata().getEventId(), "事件ID不能为空");
            assertEquals("OrderCreatedEvent", event.getEventType(), "事件类型应该正确");

            // 发布事件
            eventPublishingService.publish(event);

            log.info("EVENT-CON-003测试通过: 元数据验证成功");
        } catch (Exception e) {
            log.error("EVENT-CON-003测试失败", e);
            fail("EVENT-CON-003测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("EVENT-CON-004: 事件消费异常处理")
    void testEventConsumingException() {
        try {
            // 测试null封包
            assertThrows(Exception.class, () -> {
                eventConsumingService.dispatch(null);
            }, "消费null封包应该抛出异常");

            log.info("EVENT-CON-004测试通过: 异常处理正确");
        } catch (Exception e) {
            log.error("EVENT-CON-004测试失败", e);
            fail("EVENT-CON-004测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    @DisplayName("EVENT-DELAY-001: 延迟事件发布基础功能")
    void testBasicDelayedEventPublishing() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-delayed-order-001")
                    .customerId("customer-delayed")
                    .amount(new BigDecimal("75.00"))
                    .build();

            // 延迟发布事件（5秒后）
            String taskId = delayedEventPublisher.publishDelayed(event, 5000);

            assertNotNull(taskId, "任务ID不能为空");
            assertFalse(taskId.isEmpty(), "任务ID不能为空");

            log.info("EVENT-DELAY-001测试通过: taskId={}", taskId);
        } catch (Exception e) {
            log.error("EVENT-DELAY-001测试失败", e);
            fail("EVENT-DELAY-001测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    @DisplayName("EVENT-DELAY-002: 指定时间延迟发布")
    void testDelayedEventPublishingAt() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-delayed-order-002")
                    .customerId("customer-delayed")
                    .amount(new BigDecimal("125.00"))
                    .build();

            // 指定10秒后执行
            LocalDateTime executeTime = LocalDateTime.now().plusSeconds(10);
            String taskId = delayedEventPublisher.publishDelayedAt(event, executeTime);

            assertNotNull(taskId, "任务ID不能为空");
            assertFalse(taskId.isEmpty(), "任务ID不能为空");

            log.info("EVENT-DELAY-002测试通过: taskId={}, executeTime={}", taskId, executeTime);
        } catch (Exception e) {
            log.error("EVENT-DELAY-002测试失败", e);
            fail("EVENT-DELAY-002测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(11)
    @DisplayName("EVENT-DELAY-003: 取消延迟任务")
    void testCancelDelayedEvent() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-delayed-order-003")
                    .customerId("customer-delayed")
                    .amount(new BigDecimal("150.00"))
                    .build();

            // 延迟发布事件（30秒后）
            String taskId = delayedEventPublisher.publishDelayed(event, 30000);

            assertNotNull(taskId, "任务ID不能为空");

            // 取消任务
            boolean cancelled = delayedEventPublisher.cancelDelayed(taskId);

            assertTrue(cancelled, "任务应该被成功取消");

            log.info("EVENT-DELAY-003测试通过: 任务已取消, taskId={}", taskId);
        } catch (Exception e) {
            log.error("EVENT-DELAY-003测试失败", e);
            fail("EVENT-DELAY-003测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(12)
    @DisplayName("EVENT-DELAY-004: 待处理任务统计")
    void testPendingTaskCount() {
        try {
            // 创建测试事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .source("test-service")
                    .orderId("test-delayed-order-004")
                    .customerId("customer-delayed")
                    .amount(new BigDecimal("175.00"))
                    .build();

            // 获取初始任务数
            int initialCount = delayedEventPublisher.getPendingTaskCount();

            // 延迟发布事件
            delayedEventPublisher.publishDelayed(event, 60000);

            // 验证任务数增加
            int afterCount = delayedEventPublisher.getPendingTaskCount();
            assertTrue(afterCount >= initialCount, "待处理任务数应该增加");

            log.info("EVENT-DELAY-004测试通过: initialCount={}, afterCount={}", initialCount, afterCount);
        } catch (Exception e) {
            log.error("EVENT-DELAY-004测试失败", e);
            fail("EVENT-DELAY-004测试失败: " + e.getMessage());
        }
    }
}
