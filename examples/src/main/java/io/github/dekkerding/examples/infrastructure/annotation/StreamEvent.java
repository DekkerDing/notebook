package io.github.dekkerding.examples.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 DomainEvent 子类需要通过 Redis Stream 传输
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StreamEvent {

    /**
     * Redis Stream key，为空则按约定生成: {prefix}:{SimpleClassName}
     */
    String streamKey() default "";

    /**
     * 消费者组名称
     */
    String group() default "default-group";
}
