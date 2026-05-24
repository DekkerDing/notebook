package io.github.dekkerding.examples;

import io.github.dekkerding.examples.domain.model.ConsumerInfo;
import io.github.dekkerding.examples.infrastructure.consumer.RedisStreamEventConsumer;
import io.github.dekkerding.examples.infrastructure.consumer.StreamEventListenerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消费者管理测试：生命周期、消费者组、健康检查
 */
@Slf4j
@SpringBootTest
class StreamEventConsumerManagementTests {

    private static final String TEST_STREAM = "mgmt_test_stream";
    private static final String TEST_GROUP = "mgmt_test_group";

    @Autowired
    private RedisStreamEventConsumer eventConsumer;

    @Autowired
    private StreamEventListenerRegistry listenerRegistry;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void cleanup() {
        // 停止所有测试消费者
        List<String> active = eventConsumer.getActiveSubscriptions();
        for (String subscriptionId : active) {
            if (subscriptionId.startsWith(TEST_STREAM)) {
                String[] parts = subscriptionId.split(":");
                if (parts.length == 3) {
                    eventConsumer.stop(ConsumerInfo.of(parts[0], parts[1], parts[2]));
                }
            }
        }
        try {
            redisTemplate.delete(TEST_STREAM);
        } catch (Exception e) {
            log.warn("清理测试数据时出错: {}", e.getMessage());
        }
    }

    // ==================== 生命周期测试 ====================
    @Nested
    @DisplayName("消费者生命周期测试")
    class LifecycleTests {

        @Test
        @DisplayName("动态启动消费者")
        void testStartConsumer() {
            ConsumerInfo info = ConsumerInfo.of(TEST_STREAM, TEST_GROUP, "lifecycle-consumer-1");

            // 先发一条消息确保 Stream 存在
            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(TEST_STREAM)
                            .ofMap(java.util.Collections.singletonMap("init", "true")));

            eventConsumer.start(info);

            assertTrue(eventConsumer.isActive(info));
            List<String> active = eventConsumer.getActiveSubscriptions();
            assertTrue(active.contains(info.toSubscriptionId()));
        }

        @Test
        @DisplayName("动态停止消费者")
        void testStopConsumer() {
            ConsumerInfo info = ConsumerInfo.of(TEST_STREAM, TEST_GROUP, "lifecycle-consumer-2");

            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(TEST_STREAM)
                            .ofMap(java.util.Collections.singletonMap("init", "true")));

            eventConsumer.start(info);
            assertTrue(eventConsumer.isActive(info));

            eventConsumer.stop(info);
            assertFalse(eventConsumer.isActive(info));
        }

        @Test
        @DisplayName("重复启动消费者被阻止")
        void testDuplicateStartPrevented() {
            ConsumerInfo info = ConsumerInfo.of(TEST_STREAM, TEST_GROUP, "lifecycle-consumer-3");

            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(TEST_STREAM)
                            .ofMap(java.util.Collections.singletonMap("init", "true")));

            eventConsumer.start(info);
            int countBefore = eventConsumer.getActiveSubscriptions().size();

            // 再次启动同一个消费者
            eventConsumer.start(info);
            int countAfter = eventConsumer.getActiveSubscriptions().size();

            assertEquals(countBefore, countAfter, "重复启动不应增加消费者数量");
        }

        @Test
        @DisplayName("暂停和恢复消费者")
        void testPauseAndResume() {
            ConsumerInfo info = ConsumerInfo.of(TEST_STREAM, TEST_GROUP, "lifecycle-consumer-4");

            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(TEST_STREAM)
                            .ofMap(java.util.Collections.singletonMap("init", "true")));

            // 启动
            eventConsumer.start(info);
            assertTrue(eventConsumer.isActive(info));

            // 暂停 (停止)
            eventConsumer.stop(info);
            assertFalse(eventConsumer.isActive(info));

            // 恢复 (重新启动)
            eventConsumer.start(info);
            assertTrue(eventConsumer.isActive(info));
        }
    }

    // ==================== 消费者组测试 ====================
    @Nested
    @DisplayName("消费者组测试")
    class ConsumerGroupTests {

        @Test
        @DisplayName("批量启动多个消费者")
        void testStartMultipleConsumers() {
            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(TEST_STREAM)
                            .ofMap(java.util.Collections.singletonMap("init", "true")));

            int consumerCount = 3;
            for (int i = 1; i <= consumerCount; i++) {
                ConsumerInfo info = ConsumerInfo.of(TEST_STREAM, TEST_GROUP, "group-consumer-" + i);
                eventConsumer.start(info);
            }

            // 验证所有消费者都已启动
            List<String> active = eventConsumer.getActiveSubscriptions();
            int testConsumers = 0;
            for (String sub : active) {
                if (sub.startsWith(TEST_STREAM)) {
                    testConsumers++;
                }
            }
            assertEquals(consumerCount, testConsumers);
        }

        @Test
        @DisplayName("ConsumerInfo 值对象构建正确")
        void testConsumerInfoValueObject() {
            ConsumerInfo info = ConsumerInfo.of("my-stream", "my-group", "my-consumer");

            assertEquals("my-stream", info.getStreamKey());
            assertEquals("my-group", info.getGroupName());
            assertEquals("my-consumer", info.getConsumerName());
            assertEquals("my-stream:my-group:my-consumer", info.toSubscriptionId());
        }
    }

    // ==================== 健康检查测试 ====================
    @Nested
    @DisplayName("健康检查测试")
    class HealthTests {

        @Test
        @DisplayName("有活跃消费者时返回活跃列表")
        void testActiveConsumersReported() {
            ConsumerInfo info = ConsumerInfo.of(TEST_STREAM, TEST_GROUP, "health-consumer-1");

            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(TEST_STREAM)
                            .ofMap(java.util.Collections.singletonMap("init", "true")));

            eventConsumer.start(info);

            List<String> active = eventConsumer.getActiveSubscriptions();
            assertFalse(active.isEmpty());
        }

        @Test
        @DisplayName("消费者停止后不再出现在活跃列表")
        void testStoppedConsumerNotInActiveList() {
            ConsumerInfo info = ConsumerInfo.of(TEST_STREAM, TEST_GROUP, "health-consumer-2");

            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(TEST_STREAM)
                            .ofMap(java.util.Collections.singletonMap("init", "true")));

            eventConsumer.start(info);
            eventConsumer.stop(info);

            List<String> active = eventConsumer.getActiveSubscriptions();
            assertFalse(active.contains(info.toSubscriptionId()));
        }
    }
}

