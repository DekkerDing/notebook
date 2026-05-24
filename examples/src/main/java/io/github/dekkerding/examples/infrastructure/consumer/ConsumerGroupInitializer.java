package io.github.dekkerding.examples.infrastructure.consumer;

import io.github.dekkerding.examples.domain.model.ConsumerInfo;
import io.github.dekkerding.examples.infrastructure.routing.EventStreamRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用启动时自动初始化消费者组并启动已注册的消费者
 */
@Slf4j
public class ConsumerGroupInitializer implements SmartLifecycle {

    private final RedisStreamEventConsumer eventConsumer;
    private final StreamEventListenerRegistry listenerRegistry;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ConsumerGroupInitializer(RedisStreamEventConsumer eventConsumer,
                                    StreamEventListenerRegistry listenerRegistry) {
        this.eventConsumer = eventConsumer;
        this.listenerRegistry = listenerRegistry;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("🚀 开始初始化 Stream 事件消费者...");

            List<ConsumerInfo> consumers = listenerRegistry.getRegisteredConsumers();
            for (ConsumerInfo consumerInfo : consumers) {
                try {
                    eventConsumer.start(consumerInfo);
                } catch (Exception e) {
                    log.error("❌ 初始化消费者失败: {}", consumerInfo.toSubscriptionId(), e);
                }
            }

            log.info("✅ Stream 事件消费者初始化完成，共启动 {} 个消费者", consumers.size());
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("🛑 停止所有 Stream 事件消费者...");
            List<ConsumerInfo> consumers = listenerRegistry.getRegisteredConsumers();
            for (ConsumerInfo consumerInfo : consumers) {
                try {
                    eventConsumer.stop(consumerInfo);
                } catch (Exception e) {
                    log.error("停止消费者失败: {}", consumerInfo.toSubscriptionId(), e);
                }
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1;
    }
}
