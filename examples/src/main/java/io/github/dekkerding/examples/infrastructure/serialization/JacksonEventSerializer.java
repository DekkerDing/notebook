package io.github.dekkerding.examples.infrastructure.serialization;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dekkerding.examples.domain.event.DomainEvent;
import io.github.dekkerding.examples.domain.exception.EventSerializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 Jackson 的事件序列化实现
 */
@Slf4j
@Component
public class JacksonEventSerializer implements EventSerializer {

    private final ObjectMapper objectMapper;

    public JacksonEventSerializer() {
        this.objectMapper = createObjectMapper();
    }

    public JacksonEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper;
    }

    @Override
    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new EventSerializationException(
                    "序列化事件失败: " + event.getClass().getName(), e);
        }
    }

    @Override
    public DomainEvent deserialize(String payload, String className) {
        try {
            Class<?> eventClass = Class.forName(className);
            return (DomainEvent) objectMapper.readValue(payload, eventClass);
        } catch (ClassNotFoundException e) {
            throw new EventSerializationException(
                    "事件类不存在: " + className, e);
        } catch (Exception e) {
            throw new EventSerializationException(
                    "反序列化事件失败: " + className, e);
        }
    }
}
