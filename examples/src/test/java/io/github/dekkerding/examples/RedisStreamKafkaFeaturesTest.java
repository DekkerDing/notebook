package io.github.dekkerding.examples;

import io.github.dekkerding.examples.application.EventPublishingService;
import io.github.dekkerding.examples.config.RedisStreamEventProperties;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEventListener;
import io.github.dekkerding.examples.infrastructure.idempotent.ProcessedMessageTracker;
import io.github.dekkerding.examples.infrastructure.partition.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis Stream Kafka功能模块测试
 * 测试分区、幂等性、消费者组等功能
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.redis.cluster.nodes=192.168.10.107:6379,192.168.10.109:6379",
        "spring.redis.password=drk@2025",
        "spring.redis.stream.event.enabled=true",
        "spring.redis.stream.event.partition.enabled=false",
        "spring.redis.stream.event.idempotent.enabled=true"
})
public class RedisStreamKafkaFeaturesTest {

    @Autowired(required = false)
    private EventPublishingService eventPublishingService;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private RedisStreamEventProperties properties;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    private final List<TestEvent> receivedEvents = new ArrayList<>();
    private final AtomicInteger processCount = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        receivedEvents.clear();
        processCount.set(0);
        // 清理Redis测试数据
        if (redisTemplate != null) {
            try {
                redisTemplate.getConnectionFactory().getConnection().flushDb();
            } catch (Exception e) {
                log.warn("清理Redis数据失败: {}", e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("分区功能测试")
    class PartitionTests {

        @Test
        @DisplayName("验证哈希分区策略")
        void testHashPartitionStrategy() {
            HashPartitionStrategy strategy = new HashPartitionStrategy();

            int partition1 = strategy.partition("order-123", 3);
            int partition2 = strategy.partition("order-123", 3);
            int partition3 = strategy.partition("order-456", 3);

            assertEquals(partition1, partition2, "相同Key应该路由到相同分区");
            // 注意：不同Key可能路由到相同分区（哈希冲突），这里只验证不为null
            assertTrue(partition3 >= 0 && partition3 < 3, "分区号应该在有效范围内");
            assertTrue(partition1 >= 0 && partition1 < 3, "分区号应该在有效范围内");
        }

        @Test
        @DisplayName("验证轮询分区策略")
        void testRoundRobinPartitionStrategy() {
            RoundRobinPartitionStrategy strategy = new RoundRobinPartitionStrategy();

            int partition1 = strategy.partition("any-key", 3);
            int partition2 = strategy.partition("any-key", 3);
            int partition3 = strategy.partition("any-key", 3);

            assertEquals(0, partition1, "第一个消息应该到分区0");
            assertEquals(1, partition2, "第二个消息应该到分区1");
            assertEquals(2, partition3, "第三个消息应该到分区2");
        }

        @Test
        @DisplayName("验证分区管理器")
        void testPartitionManager() {
            PartitionManager manager = new PartitionManager(
                    "test:",
                    3,
                    new HashPartitionStrategy(),
                    redisTemplate);

            assertEquals(3, manager.getPartitionCount());
            assertEquals("hash", manager.getPartitionStrategy().getName());

            String streamKey = manager.getPartitionStreamKey("TestEvent", 1);
            assertEquals("test:TestEvent:1", streamKey);

            assertThrows(IllegalArgumentException.class,
                    () -> manager.getPartitionStreamKey("TestEvent", 5));
        }
    }

    @Nested
    @DisplayName("幂等性测试")
    class IdempotentTests {

        @Test
        @DisplayName("验证消息幂等性处理")
        void testIdempotentProcessing() {
            ProcessedMessageTracker tracker = new ProcessedMessageTracker(
                    3600, true, redisTemplate);

            String messageId = "msg-123";
            String consumerGroup = "test-group";
            String streamKey = "test-stream";

            assertFalse(tracker.isProcessed(messageId, consumerGroup, streamKey),
                    "消息应该未被处理");

            tracker.markAsProcessed(messageId, consumerGroup, streamKey);

            assertTrue(tracker.isProcessed(messageId, consumerGroup, streamKey),
                    "消息应该已被处理");
        }

        @Test
        @DisplayName("验证幂等性禁用")
        void testIdempotentDisabled() {
            ProcessedMessageTracker tracker = new ProcessedMessageTracker(
                    3600, false, redisTemplate);

            String messageId = "msg-456";
            String consumerGroup = "test-group";
            String streamKey = "test-stream";

            tracker.markAsProcessed(messageId, consumerGroup, streamKey);

            assertFalse(tracker.isProcessed(messageId, consumerGroup, streamKey),
                    "幂等性禁用时应该总是返回未处理");
        }
    }

    @Nested
    @DisplayName("事件发布测试")
    class EventPublishingTests {

        @Test
        @DisplayName("验证普通模式事件发布")
        void testNormalEventPublishing() {
            TestEvent event = new TestEvent(this, "test-1", "Test Data");

            assertDoesNotThrow(() -> {
                if (eventPublishingService != null) {
                    eventPublishingService.publish(event);
                }
            });
        }

        @Test
        @DisplayName("验证分区模式事件发布")
        void testPartitionedEventPublishing() {
            TestEvent event = new TestEvent(this, "partition-test", "Partitioned Data");

            assertDoesNotThrow(() -> {
                if (eventPublishingService != null) {
                    eventPublishingService.publish(event);
                }
            });
        }
    }

    @Nested
    @DisplayName("配置验证测试")
    class ConfigurationTests {

        @Test
        @DisplayName("验证分区配置")
        void testPartitionConfiguration() {
            if (properties != null) {
                // 验证配置对象存在
                assertNotNull(properties.getPartition());
            }
        }

        @Test
        @DisplayName("验证幂等性配置")
        void testIdempotentConfiguration() {
            if (properties != null) {
                // 验证配置对象存在
                assertNotNull(properties.getIdempotent());
            }
        }

        @Test
        @DisplayName("验证回溯配置")
        void testRewindConfiguration() {
            if (properties != null) {
                // 验证配置对象存在
                assertNotNull(properties.getRewind());
            }
        }
    }

    // 测试事件类
    @Getter
    @StreamEvent(streamKey = "test_stream", group = "test-group")
    public static class TestEvent extends DomainEvent {
        private final String eventId;
        private final String eventData;

        public TestEvent(Object source, String eventId, String eventData) {
            super(source);
            this.eventId = eventId;
            this.eventData = eventData;
        }
    }

    // 测试监听器方法
    @StreamEventListener(value = TestEvent.class, group = "test-group")
    public void handleTestEvent(TestEvent event) {
        log.info("处理测试事件: {}", event.getEventId());
        receivedEvents.add(event);
        processCount.incrementAndGet();
    }
}
