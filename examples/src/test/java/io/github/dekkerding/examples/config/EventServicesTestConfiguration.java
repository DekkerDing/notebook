package io.github.dekkerding.examples.config;

import io.github.dekkerding.examples.application.EventConsumingService;
import io.github.dekkerding.examples.application.EventPublishingService;
import io.github.dekkerding.examples.application.DelayedEventPublisher;
import io.github.dekkerding.examples.infrastructure.delayed.DelayedTaskDispatcher;
import io.github.dekkerding.examples.infrastructure.delayed.TimingWheelScheduler;
import io.github.dekkerding.examples.infrastructure.idempotent.IdempotentConsumer;
import io.github.dekkerding.examples.infrastructure.idempotent.ProcessedMessageTracker;
import io.github.dekkerding.examples.infrastructure.partition.HashPartitionStrategy;
import io.github.dekkerding.examples.infrastructure.partition.PartitionManager;
import io.github.dekkerding.examples.infrastructure.partition.PartitionStrategy;
import io.github.dekkerding.examples.infrastructure.publisher.PartitionedStreamPublisher;
import io.github.dekkerding.examples.infrastructure.publisher.RedisStreamEventPublisher;
import io.github.dekkerding.examples.infrastructure.routing.EventStreamRouter;
import io.github.dekkerding.examples.infrastructure.serialization.EventSerializer;
import io.github.dekkerding.examples.infrastructure.serialization.JacksonEventSerializer;
import io.github.dekkerding.examples.producer.BusinessProcessorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 事件服务测试配置
 *
 * <p>为测试环境提供事件服务相关的Bean配置
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@TestConfiguration
@Profile("test")
public class EventServicesTestConfiguration {

    /**
     * 事件序列化器
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public EventSerializer eventSerializer() {
        return new JacksonEventSerializer();
    }

    /**
     * 事件Stream路由器
     */
    @Bean
    @ConditionalOnMissingBean
    public EventStreamRouter eventStreamRouter() {
        return new EventStreamRouter("test:stream:", null);
    }

    /**
     * 分区策略
     */
    @Bean
    @ConditionalOnMissingBean
    public PartitionStrategy partitionStrategy() {
        return new HashPartitionStrategy();
    }

    /**
     * 分区管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public PartitionManager partitionManager(
            PartitionStrategy partitionStrategy,
            RedisTemplate<String, String> redisTemplate) {
        return new PartitionManager("test:partition:", 4, partitionStrategy, redisTemplate);
    }

    /**
     * Redis Stream事件发布器
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisStreamEventPublisher redisStreamEventPublisher(
            RedisTemplate<String, String> redisTemplate) {
        return new RedisStreamEventPublisher(redisTemplate);
    }

    /**
     * 分区Stream发布器
     */
    @Bean
    @ConditionalOnMissingBean
    public PartitionedStreamPublisher partitionedStreamPublisher(
            RedisTemplate<String, String> redisTemplate,
            PartitionManager partitionManager) {
        return new PartitionedStreamPublisher(redisTemplate, partitionManager);
    }

    /**
     * 事件发布服务
     */
    @Bean
    @ConditionalOnMissingBean
    public EventPublishingService eventPublishingService(
            EventSerializer serializer,
            EventStreamRouter router,
            RedisStreamEventPublisher publisher,
            PartitionedStreamPublisher partitionedPublisher) {
        // 禁用分区模式，使用普通发布器
        return new EventPublishingService(serializer, router, publisher);
    }

    /**
     * 已处理消息跟踪器
     */
    @Bean
    @ConditionalOnMissingBean
    public ProcessedMessageTracker processedMessageTracker(
            RedisTemplate<String, String> redisTemplate) {
        return new ProcessedMessageTracker(3600, false, redisTemplate);
    }

    /**
     * 幂等消费者
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentConsumer idempotentConsumer(ProcessedMessageTracker tracker) {
        return new IdempotentConsumer(tracker, false);
    }

    /**
     * 业务处理器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public BusinessProcessorFactory businessProcessorFactory() {
        return new BusinessProcessorFactory(null);
    }

    /**
     * 事件消费服务
     */
    @Bean
    @ConditionalOnMissingBean
    public EventConsumingService eventConsumingService(
            EventSerializer serializer,
            ApplicationEventPublisher eventPublisher,
            BusinessProcessorFactory processorFactory,
            IdempotentConsumer idempotentConsumer) {
        return new EventConsumingService(serializer, eventPublisher, processorFactory, idempotentConsumer);
    }

    /**
     * 延迟任务分发器
     */
    @Bean
    @ConditionalOnMissingBean
    public DelayedTaskDispatcher delayedTaskDispatcher() {
        return new DelayedTaskDispatcher();
    }

    /**
     * 时间轮调度器
     */
    @Bean
    @ConditionalOnMissingBean
    public TimingWheelScheduler timingWheelScheduler(DelayedTaskDispatcher taskDispatcher) {
        return new TimingWheelScheduler(taskDispatcher);
    }

    /**
     * 延迟事件发布器
     */
    @Bean
    @ConditionalOnMissingBean
    public DelayedEventPublisher delayedEventPublisher(
            TimingWheelScheduler timingWheelScheduler,
            DelayedTaskDispatcher taskDispatcher) {
        return new DelayedEventPublisher(timingWheelScheduler, taskDispatcher);
    }
}
