package io.github.dekkerding.examples.infrastructure.routing;

import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.infrastructure.annotation.StreamEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件类型到 Redis Stream Key 的路由映射
 * 解析优先级: @StreamEvent注解 > yaml配置 > 约定({prefix}:{SimpleClassName})
 */
@Slf4j
public class EventStreamRouter {

    private final String keyPrefix;
    private final Map<String, String> configuredRoutes;
    private final Map<Class<?>, String> routeCache = new ConcurrentHashMap<>();

    public EventStreamRouter(String keyPrefix, Map<String, String> configuredRoutes) {
        this.keyPrefix = keyPrefix != null ? keyPrefix : "stream:";
        this.configuredRoutes = configuredRoutes != null ? configuredRoutes : new ConcurrentHashMap<>();
    }

    public String resolveStreamKey(Class<? extends DomainEvent> eventClass) {
        return routeCache.computeIfAbsent(eventClass, clazz -> doResolve(eventClass));
    }

    private String doResolve(Class<? extends DomainEvent> eventClass) {
        // 优先级1: @StreamEvent 注解
        StreamEvent annotation = eventClass.getAnnotation(StreamEvent.class);
        if (annotation != null && StringUtils.isNotBlank(annotation.streamKey())) {
            log.debug("路由解析 [注解]: {} -> {}", eventClass.getSimpleName(), annotation.streamKey());
            return annotation.streamKey();
        }

        // 优先级2: yaml 配置
        String simpleName = eventClass.getSimpleName();
        String configuredKey = configuredRoutes.get(simpleName);
        if (StringUtils.isNotBlank(configuredKey)) {
            log.debug("路由解析 [配置]: {} -> {}", simpleName, configuredKey);
            return configuredKey;
        }

        // 优先级3: 约定
        String conventionKey = keyPrefix + simpleName;
        log.debug("路由解析 [约定]: {} -> {}", simpleName, conventionKey);
        return conventionKey;
    }

    public String resolveGroup(Class<? extends DomainEvent> eventClass) {
        StreamEvent annotation = eventClass.getAnnotation(StreamEvent.class);
        if (annotation != null && StringUtils.isNotBlank(annotation.group())) {
            return annotation.group();
        }
        return "default-group";
    }
}
