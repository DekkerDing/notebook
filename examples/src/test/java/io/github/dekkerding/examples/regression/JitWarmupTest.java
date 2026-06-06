package io.github.dekkerding.examples.regression;

import io.github.dekkerding.examples.application.JitWarmupRunner;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.compression.AdaptiveCompressionStrategy;
import io.github.dekkerding.examples.infrastructure.serialization.EventSerializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j

/**
 * JIT预热测试
 *
 * <p>测试JIT预热优化功能
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("JIT预热测试")
public class JitWarmupTest {

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private EventSerializer eventSerializer;

    @Autowired(required = false)
    private AdaptiveCompressionStrategy compressionStrategy;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(applicationContext != null, "ApplicationContext 未配置");
    }

    @Test
    @Order(1)
    @DisplayName("JIT-001: 验证JIT预热组件存在")
    void testJitWarmupComponentsExist() {
        try {
            // 验证JIT预热组件存在
            JitWarmupRunner warmupRunner = applicationContext.getBean(JitWarmupRunner.class);
            assertNotNull(warmupRunner, "JitWarmupRunner 应该被注入");

            assertNotNull(eventSerializer, "EventSerializer 应该被注入");
            assertNotNull(compressionStrategy, "AdaptiveCompressionStrategy 应该被注入");

            log.info("JIT-001测试通过: 所有组件存在");
        } catch (Exception e) {
            log.error("JIT-001测试失败", e);
            fail("JIT-001测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("JIT-002: 序列化性能测试")
    void testSerializationPerformance() {
        Assumptions.assumeTrue(eventSerializer != null, "EventSerializer 未配置");

        try {
            // 创建测试事件
            TestEvent testEvent = new TestEvent("test-source");

            // 预热100次
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                eventSerializer.serialize(testEvent);
            }
            long warmupDuration = System.currentTimeMillis() - startTime;

            // 测试100次
            startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                eventSerializer.serialize(testEvent);
            }
            long testDuration = System.currentTimeMillis() - startTime;

            assertTrue(warmupDuration > 0, "预热时间应该大于0");
            assertTrue(testDuration > 0, "测试时间应该大于0");

            log.info("JIT-002测试通过: warmup={}ms, test={}ms", warmupDuration, testDuration);
        } catch (Exception e) {
            log.error("JIT-002测试失败", e);
            fail("JIT-002测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("JIT-003: 压缩性能测试")
    void testCompressionPerformance() {
        Assumptions.assumeTrue(compressionStrategy != null, "AdaptiveCompressionStrategy 未配置");

        try {
            // 创建测试数据
            String testPayload = createTestPayload(1024);

            // 预热100次
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                compressionStrategy.compress(testPayload);
            }
            long warmupDuration = System.currentTimeMillis() - startTime;

            // 测试100次
            startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                compressionStrategy.compress(testPayload);
            }
            long testDuration = System.currentTimeMillis() - startTime;

            assertTrue(warmupDuration > 0, "预热时间应该大于0");
            assertTrue(testDuration > 0, "测试时间应该大于0");

            log.info("JIT-003测试通过: warmup={}ms, test={}ms", warmupDuration, testDuration);
        } catch (Exception e) {
            log.error("JIT-003测试失败", e);
            fail("JIT-003测试失败: " + e.getMessage());
        }
    }

    /**
     * 创建测试负载
     */
    private String createTestPayload(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append("a");
        }
        return sb.toString();
    }

    /**
     * 测试事件类
     */
    @lombok.Data
    @lombok.EqualsAndHashCode(callSuper = false)
    static class TestEvent extends DomainEvent {
        private String eventId;
        private long eventTimestamp;

        public TestEvent(String source) {
            super(source);
            this.eventId = "test-event-" + System.currentTimeMillis();
            this.eventTimestamp = System.currentTimeMillis();
        }
    }
}
