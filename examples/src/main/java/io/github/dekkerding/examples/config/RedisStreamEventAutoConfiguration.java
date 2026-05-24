package io.github.dekkerding.examples.config;

import io.github.dekkerding.examples.application.EventConsumingService;
import io.github.dekkerding.examples.application.EventPublishingService;
import io.github.dekkerding.examples.application.handler.StreamEventApplicationListener;
import io.github.dekkerding.examples.infrastructure.consumer.ConsumerGroupInitializer;
import io.github.dekkerding.examples.infrastructure.consumer.RedisStreamEventConsumer;
import io.github.dekkerding.examples.infrastructure.consumer.StreamEventListenerRegistry;
import io.github.dekkerding.examples.infrastructure.health.StreamConsumerHealthIndicator;
import io.github.dekkerding.examples.infrastructure.idempotent.IdempotentConsumer;
import io.github.dekkerding.examples.infrastructure.idempotent.ProcessedMessageTracker;
import io.github.dekkerding.examples.infrastructure.partition.*;
import io.github.dekkerding.examples.infrastructure.publisher.PartitionedStreamPublisher;
import io.github.dekkerding.examples.infrastructure.publisher.RedisStreamEventPublisher;
import io.github.dekkerding.examples.infrastructure.retry.RetryPolicy;
import io.github.dekkerding.examples.infrastructure.retry.RetryableStreamConsumer;
import io.github.dekkerding.examples.infrastructure.routing.EventStreamRouter;
import io.github.dekkerding.examples.infrastructure.serialization.EventSerializer;
import io.github.dekkerding.examples.infrastructure.serialization.JacksonEventSerializer;
import io.github.dekkerding.examples.producer.BusinessProcessorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

/**
 * Redis Stream 事件桥接自动配置
 * 增强版：支持分区、幂等性、负载均衡等Kafka功能
 */
@Configuration
@EnableConfigurationProperties(RedisStreamEventProperties.class)
@ConditionalOnProperty(prefix = "spring.redis.stream.event", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisStreamEventAutoConfiguration {

    @Bean
    @Primary
    public EventSerializer eventSerializer() {
        return new JacksonEventSerializer();
    }

    @Bean
    public EventStreamRouter eventStreamRouter(RedisStreamEventProperties properties) {
        return new EventStreamRouter(properties.getKeyPrefix(), properties.getRoutes());
    }

    @Bean
    public RetryPolicy retryPolicy(RedisStreamEventProperties properties) {
        RedisStreamEventProperties.RetryProperties retry = properties.getRetry();
        return RetryPolicy.builder()
                .maxAttempts(retry.getMaxAttempts())
                .initialInterval(retry.getInitialInterval())
                .backoffMultiplier(retry.getBackoffMultiplier())
                .build();
    }

    @Bean
    public PartitionStrategy partitionStrategy(RedisStreamEventProperties properties) {
        String strategyName = properties.getPartition().getStrategy().toLowerCase();
        switch (strategyName) {
            case "round_robin":
                return new RoundRobinPartitionStrategy();
            case "hash":
            default:
                return new HashPartitionStrategy();
        }
    }

    @Bean
    public PartitionManager partitionManager(
            PartitionStrategy partitionStrategy,
            RedisStreamEventProperties properties,
            RedisTemplate<String, String> redisTemplate) {
        return new PartitionManager(
                properties.getKeyPrefix(),
                properties.getPartition().getCount(),
                partitionStrategy,
                redisTemplate);
    }

    @Bean
    public ProcessedMessageTracker processedMessageTracker(
            RedisStreamEventProperties properties,
            RedisTemplate<String, String> redisTemplate) {
        return new ProcessedMessageTracker(
                properties.getIdempotent().getKeyTtl(),
                properties.getIdempotent().isEnabled(),
                redisTemplate);
    }

    @Bean
    public IdempotentConsumer idempotentConsumer(
            ProcessedMessageTracker tracker,
            RedisStreamEventProperties properties) {
        return new IdempotentConsumer(
                tracker,
                properties.getIdempotent().isEnabled());
    }

    @Bean
    public RedisStreamEventPublisher redisStreamEventPublisher(
            RedisTemplate<String, String> redisTemplate) {
        return new RedisStreamEventPublisher(redisTemplate);
    }

    @Bean
    public PartitionedStreamPublisher partitionedStreamPublisher(
            RedisTemplate<String, String> redisTemplate,
            PartitionManager partitionManager) {
        return new PartitionedStreamPublisher(redisTemplate, partitionManager);
    }

    @Bean
    public EventPublishingService eventPublishingService(
            EventSerializer serializer,
            EventStreamRouter router,
            RedisStreamEventPublisher publisher,
            PartitionedStreamPublisher partitionedPublisher,
            RedisStreamEventProperties properties) {
        return new EventPublishingService(serializer, router, publisher, partitionedPublisher, properties);
    }

    @Bean
    public StreamEventApplicationListener streamEventApplicationListener(
            EventPublishingService publishingService) {
        return new StreamEventApplicationListener(publishingService);
    }

    @Bean
    public RetryableStreamConsumer retryableStreamConsumer(
            RedisTemplate<String, String> redisTemplate,
            RetryPolicy retryPolicy,
            RedisStreamEventProperties properties) {
        return new RetryableStreamConsumer(
                redisTemplate, retryPolicy, properties.getDlq().getKeyPrefix());
    }

    @Bean
    public EventConsumingService eventConsumingService(
            EventSerializer serializer,
            ApplicationEventPublisher eventPublisher,
            BusinessProcessorFactory processorFactory,
            IdempotentConsumer idempotentConsumer) {
        return new EventConsumingService(serializer, eventPublisher, processorFactory, idempotentConsumer);
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
    streamEventListenerContainer(RedisConnectionFactory factory,
                                 RedisStreamEventProperties properties) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofMillis(properties.getPollTimeout()))
                        .batchSize(properties.getBatchSize())
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(factory, options);
        container.start();
        return container;
    }

    @Bean
    public RedisStreamEventConsumer redisStreamEventConsumer(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamEventListenerContainer,
            RedisTemplate<String, String> redisTemplate,
            EventConsumingService consumingService,
            RetryableStreamConsumer retryableConsumer) {
        return new RedisStreamEventConsumer(
                streamEventListenerContainer, redisTemplate, consumingService, retryableConsumer);
    }

    @Bean
    public StreamEventListenerRegistry streamEventListenerRegistry(
            EventStreamRouter router,
            RedisStreamEventProperties properties) {
        return new StreamEventListenerRegistry(router, properties.getConsumerName());
    }

    @Bean
    public ConsumerGroupInitializer consumerGroupInitializer(
            RedisStreamEventConsumer eventConsumer,
            StreamEventListenerRegistry listenerRegistry) {
        return new ConsumerGroupInitializer(eventConsumer, listenerRegistry);
    }

    @Bean
    public StreamConsumerHealthIndicator streamConsumerHealthIndicator(
            RedisStreamEventConsumer eventConsumer,
            ProcessedMessageTracker tracker) {
        return new StreamConsumerHealthIndicator(eventConsumer, tracker);
    }
}
