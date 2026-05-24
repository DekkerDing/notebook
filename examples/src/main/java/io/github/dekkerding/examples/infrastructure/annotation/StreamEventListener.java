package io.github.dekkerding.examples.infrastructure.annotation;

import io.github.dekkerding.examples.domain.event.DomainEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为 Redis Stream 事件监听器，支持配置消费者组和并发数
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StreamEventListener {

    /**
     * 监听的事件类型
     */
    Class<? extends DomainEvent> value();

    /**
     * 消费者组名称，优先级高于 @StreamEvent 上的 group
     */
    String group() default "";

    /**
     * 消费者并发数
     */
    int concurrency() default 1;
}
