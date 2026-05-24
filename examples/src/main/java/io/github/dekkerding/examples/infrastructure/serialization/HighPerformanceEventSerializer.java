package io.github.dekkerding.examples.infrastructure.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.domain.exception.EventSerializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高性能事件序列化器
 * 使用缓存和优化的ObjectMapper提升性能
 */
@Slf4j
@Component
public class HighPerformanceEventSerializer implements EventSerializer {

    private final ObjectMapper objectMapper;
    private final Map<String, Class<?>> typeCache = new ConcurrentHashMap<>();

    public HighPerformanceEventSerializer() {
        this.objectMapper = new ObjectMapper();
        registerCommonTypes();
    }

    @Override
    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new EventSerializationException("事件序列化失败: " + event.getClass().getName(), e);
        }
    }

    @Override
    public DomainEvent deserialize(String payload, String className) {
        try {
            Class<?> clazz = typeCache.computeIfAbsent(className, key -> {
                try {
                    return Class.forName(key);
                } catch (ClassNotFoundException e) {
                    throw new EventSerializationException("类未找到: " + key, e);
                }
            });

            return (DomainEvent) objectMapper.readValue(payload, clazz);
        } catch (Exception e) {
            throw new EventSerializationException("事件反序列化失败: " + className, e);
        }
    }

    /**
     * 批量序列化优化版本
     */
    public Map<String, String> serializeBatch(Map<String, DomainEvent> events) {
        Map<String, String> result = new ConcurrentHashMap<>(events.size());
        events.forEach((key, event) -> {
            try {
                result.put(key, serialize(event));
            } catch (Exception e) {
                log.error("批量序列化失败: {}", key, e);
            }
        });
        return result;
    }

    /**
     * 预注册常用类型
     */
    private void registerCommonTypes() {
        String[] commonTypes = {
                "io.github.dekkerding.examples.event.OrderCreatedEvent",
                "io.github.dekkerding.examples.event.OrderStatusChangedEvent",
                "io.github.dekkerding.examples.event.PaymentCompletedEvent",
                "io.github.dekkerding.examples.event.PaymentFailedEvent",
                "io.github.dekkerding.examples.event.ShipmentCreatedEvent"
        };

        for (String className : commonTypes) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException ignored) {
                // 类型不存在，忽略
            }
        }

        log.info("高性能序列化器已初始化，预注册{}个常用类型", commonTypes.length);
    }

    /**
     * 清理类型缓存
     */
    public void clearTypeCache() {
        typeCache.clear();
        log.info("类型缓存已清理");
    }

    /**
     * 获取类型缓存统计
     */
    public int getTypeCacheSize() {
        return typeCache.size();
    }
}
