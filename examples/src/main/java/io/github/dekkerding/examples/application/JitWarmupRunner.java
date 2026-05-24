package io.github.dekkerding.examples.application;

import io.github.dekkerding.examples.domain.event.OrderCreatedEvent;
import io.github.dekkerding.examples.domain.event.PaymentCompletedEvent;
import io.github.dekkerding.examples.infrastructure.compression.AdaptiveCompressionStrategy;
import io.github.dekkerding.examples.infrastructure.serialization.EventSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * JIT预热优化器
 *
 * <p>在应用启动时预热关键代码路径，使JIT编译器提前优化热点代码，
 * 保证启动后性能稳定。
 *
 * <p>预热目标：
 * <ul>
 *   <li>Jackson序列化器（1000次迭代）</li>
 *   <li>压缩算法（1000次迭代）</li>
 *   <li>分区策略计算（5000次迭代）</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE) // 最后执行
public class JitWarmupRunner implements ApplicationRunner {

    private final EventSerializer serializer;
    private final AdaptiveCompressionStrategy compression;

    public JitWarmupRunner(EventSerializer serializer,
                           AdaptiveCompressionStrategy compression) {
        this.serializer = serializer;
        this.compression = compression;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始JIT预热...");
        long start = System.currentTimeMillis();

        try {
            // 预热序列化（1000次）
            warmupSerialization();

            // 预热压缩（1000次）
            warmupCompression();

            // 预热分区策略（5000次）
            warmupPartitionStrategy();

            long duration = System.currentTimeMillis() - start;
            log.info("JIT预热完成，耗时: {}ms", duration);

        } catch (Exception e) {
            log.warn("JIT预热失败，将继续运行: {}", e.getMessage());
        }
    }

    /**
     * 预热序列化
     */
    private void warmupSerialization() {
        log.info("预热序列化...");
        Object source = new Object();
        OrderCreatedEvent testEvent = OrderCreatedEvent.builder()
                .source(source)
                .orderId("warmup-test-123")
                .customerId("warmup-customer")
                .amount(java.math.BigDecimal.valueOf(100))
                .build();

        for (int i = 0; i < 1000; i++) {
            try {
                serializer.serialize(testEvent);
            } catch (Exception e) {
                // 忽略序列化错误
            }
        }
    }

    /**
     * 预热压缩
     */
    private void warmupCompression() {
        log.trace("预热压缩...");
        String testPayload = createTestPayload(1024);

        for (int i = 0; i < 1000; i++) {
            try {
                compression.compress(testPayload);
            } catch (Exception e) {
                // 忽略压缩错误
            }
        }
    }

    /**
     * 预热分区策略
     */
    private void warmupPartitionStrategy() {
        log.trace("预热分区策略...");
        String[] testKeys = new String[100];

        for (int i = 0; i < 100; i++) {
            testKeys[i] = "key-" + i;
        }

        // 模拟哈希计算
        for (int i = 0; i < 5000; i++) {
            String key = testKeys[i % testKeys.length];
            int partition = Math.abs(key.hashCode()) % 3;
        }
    }

    /**
     * 创建测试负载
     */
    private String createTestPayload(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
