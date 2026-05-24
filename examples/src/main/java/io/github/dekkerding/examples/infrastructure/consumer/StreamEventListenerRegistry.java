package io.github.dekkerding.examples.infrastructure.consumer;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.domain.model.ConsumerInfo;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEventListener;
import io.github.dekkerding.examples.infrastructure.routing.EventStreamRouter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 扫描 @StreamEventListener 注解的方法，注册消费者信息
 */
@Slf4j
public class StreamEventListenerRegistry implements BeanPostProcessor {

    private final EventStreamRouter router;
    private final String defaultConsumerName;

    @Getter
    private final List<ConsumerInfo> registeredConsumers = new CopyOnWriteArrayList<>();

    public StreamEventListenerRegistry(EventStreamRouter router, String defaultConsumerName) {
        this.router = router;
        this.defaultConsumerName = defaultConsumerName;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        for (Method method : beanClass.getDeclaredMethods()) {
            StreamEventListener annotation = method.getAnnotation(StreamEventListener.class);
            if (annotation == null) {
                continue;
            }

            Class<? extends DomainEvent> eventClass = annotation.value();
            String streamKey = router.resolveStreamKey(eventClass);

            // 解析 group: 方法注解 > 事件类注解 > 默认
            String group = resolveGroup(annotation, eventClass);

            int concurrency = annotation.concurrency();
            for (int i = 1; i <= concurrency; i++) {
                String consumerName = defaultConsumerName + "-" + beanName + "-" + i;
                ConsumerInfo consumerInfo = ConsumerInfo.of(streamKey, group, consumerName);
                registeredConsumers.add(consumerInfo);

                log.info("📋 注册事件监听器: method={}.{}, event={}, stream={}, group={}, consumer={}",
                        beanClass.getSimpleName(), method.getName(),
                        eventClass.getSimpleName(), streamKey, group, consumerName);
            }
        }

        return bean;
    }

    private String resolveGroup(StreamEventListener listenerAnnotation,
                                Class<? extends DomainEvent> eventClass) {
        // 优先级1: @StreamEventListener 上的 group
        if (StringUtils.isNotBlank(listenerAnnotation.group())) {
            return listenerAnnotation.group();
        }

        // 优先级2: @StreamEvent 上的 group
        return router.resolveGroup(eventClass);
    }
}
